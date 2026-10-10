# Provider 설정
provider "aws" {
  region = "ap-northeast-2"
}

# VPC 생성
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "todoing-vpc"
  }
}

# 인터넷 게이트웨이
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "todoing-igw"
  }
}

# Public 서브넷
resource "aws_subnet" "public_1" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.3.0/24"
  availability_zone       = "ap-northeast-2a"
  map_public_ip_on_launch = true

  tags = {
    Name = "todoing-public-1"
  }
}

# 두 번째 Public 서브넷
resource "aws_subnet" "public_2" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.4.0/24"
  availability_zone       = "ap-northeast-2c"
  map_public_ip_on_launch = true

  tags = {
    Name = "todoing-public-2"
  }
}

# Public 라우팅 테이블
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "todoing-public-rt"
  }
}

# Public 서브넷 라우팅 연결
resource "aws_route_table_association" "public_1" {
  subnet_id      = aws_subnet.public_1.id
  route_table_id = aws_route_table.public.id
}

# Public 서브넷 라우팅 연결 (두 번째 서브넷용)
resource "aws_route_table_association" "public_2" {
  subnet_id      = aws_subnet.public_2.id
  route_table_id = aws_route_table.public.id
}

# 보안 그룹 - EC2
resource "aws_security_group" "ec2" {
  name        = "todoing-ec2-sg"
  description = "Security group for EC2"
  vpc_id      = aws_vpc.main.id

  # Spring Boot 애플리케이션용 8080 포트
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow Spring Boot application"
  }

  # SSH 접속
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Prometheus
  ingress {
    from_port   = 9090
    to_port     = 9090
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Grafana
  ingress {
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # 아웃바드 트래픽 허용
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "todoing-ec2-sg"
  }
}

# IAM 역할 생성
resource "aws_iam_role" "ec2_role" {
  name = "todoing-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

# SSM 관리형 정책 연결
resource "aws_iam_role_policy_attachment" "ssm_policy" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# ECR 정책 추가
resource "aws_iam_role_policy" "ecr_policy" {
  name = "ecr-policy"
  role = aws_iam_role.ec2_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage"
        ]
        Resource = "*"
      }
    ]
  })
}

# EC2용 인스턴스 프로파일 생성
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "todoing-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# EC2 인스턴스
resource "aws_instance" "app" {
  ami           = "ami-0e9bfdb247cc8de84" # Ubuntu 22.04 LTS AMI
  instance_type = "t3.micro"
  subnet_id     = aws_subnet.public_1.id

  # 세부(1분 간격) 모니터링은 프리티어 미포함 과금 대상이라 비활성화(기본값=false, 5분 간격 기본 모니터링은 무료)
  monitoring = false

  vpc_security_group_ids = [aws_security_group.ec2.id]
  key_name               = "todoing-key"
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  user_data = <<-EOF
              #!/bin/bash
              set -e  # 오류 발생 시 스크립트 중단

              # SSM Agent 설치
              sudo snap install amazon-ssm-agent --classic
              sudo systemctl start snap.amazon-ssm-agent.amazon-ssm-agent.service
              sudo systemctl enable snap.amazon-ssm-agent.amazon-ssm-agent.service

              # 스왑 2GB 생성 — t3.micro(RAM 1GB)에서 Redis+Prometheus+Grafana+앱을 동시에
              # 띄우면 메모리가 빠듯해서, 일시적 메모리 스파이크에 OOM killer가 바로 프로세스를
              # 죽이지 않도록 여유를 둔다(지속적인 메모리 부족 자체를 없애주진 않음).
              sudo fallocate -l 2G /swapfile
              sudo chmod 600 /swapfile
              sudo mkswap /swapfile
              sudo swapon /swapfile
              echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

              # AWS CLI 설치
              sudo apt-get update
              sudo apt-get install -y unzip
              curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
              unzip awscliv2.zip
              sudo ./aws/install

              # Docker 설치
              sudo apt-get install -y ca-certificates curl gnupg
              sudo install -m 0755 -d /etc/apt/keyrings
              curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
              sudo chmod a+r /etc/apt/keyrings/docker.gpg
              echo "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
              sudo apt-get update
              sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
              sudo usermod -aG docker ubuntu
              sudo systemctl start docker
              sudo systemctl enable docker

              # Docker Compose 설치
              sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
              sudo chmod +x /usr/local/bin/docker-compose

              # CloudWatch Agent 설치 및 설정
              sudo wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
              sudo dpkg -i amazon-cloudwatch-agent.deb

              # CloudWatch Agent 설정
              sudo mkdir -p /opt/aws/amazon-cloudwatch-agent/etc/
              sudo bash -c 'cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json' << 'EOT'
              {
                "agent": {
                  "metrics_collection_interval": 60,
                  "run_as_user": "root"
                },
                "metrics": {
                  "append_dimensions": {
                    "InstanceId": "$${aws:InstanceId}",
                    "InstanceType": "$${aws:InstanceType}",
                    "AutoScalingGroupName": "$${aws:AutoScalingGroupName}"
                  },
                  "metrics_collected": {
                    "mem": {
                      "measurement": [
                        "mem_used_percent"
                      ],
                      "metrics_collection_interval": 60
                    },
                    "swap": {
                      "measurement": [
                        "swap_used_percent"
                      ]
                    }
                  }
                }
              }
              EOT

              # CloudWatch Agent 시작
              sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
              sudo systemctl start amazon-cloudwatch-agent
              sudo systemctl enable amazon-cloudwatch-agent
              EOF

  tags = {
    Name = "todoing-app"
  }
}

# 탄력적 IP
resource "aws_eip" "app" {
  instance = aws_instance.app.id

  tags = {
    Name = "todoing-app-eip"
  }
}

# 출력값
output "public_ip" {
  value = aws_eip.app.public_ip
}

output "ssh_command" {
  value = "ssh -i todoing-key.pem ubuntu@${aws_eip.app.public_ip}"
}

output "instance_id" {
  value       = aws_instance.app.id
  description = "EC2 인스턴스 ID"
}

# ECR 저장소 생성
resource "aws_ecr_repository" "app" {
  name         = "backend-todoing"
  force_delete = true # 이 줄 추가

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "backend-todoing"
  }
}

# ECR 저장소 정책은 따로 두지 않음 — 리포지토리 정책 없이도 같은 계정의 IAM 주체(ec2_role 등)는
# 계정 레벨 IAM 권한(아래 ecr_policy)만으로 push/pull 가능하다. Principal을 "*"로 여는 정책은
# 전 세계 아무 AWS 계정이나 push할 수 있게 해서(ECR 스토리지 과금 남용 + 이미지 변조 위험) 삭제함.

# 출력값 추가 (선택사항)
output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

# 키 페어 생성
resource "aws_key_pair" "todoing" {
  key_name   = "todoing-key"
  public_key = file("${path.module}/todoing-key.pub") # 로컬에 있는 public key 파일 경로
}

# RDS 보안 그룹
resource "aws_security_group" "rds" {
  name        = "todoing-rds-sg"
  description = "Security group for RDS"
  vpc_id      = aws_vpc.main.id

  # 외부에서의 MySQL 접속 허용
  ingress {
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] # 모든 IP에서 접근 가능
  }

  tags = {
    Name = "todoing-rds-sg"
  }
}

# RDS 서브넷 그룹
resource "aws_db_subnet_group" "rds" {
  name       = "todoing-rds-subnet-group"
  subnet_ids = [aws_subnet.public_1.id, aws_subnet.public_2.id]

  tags = {
    Name = "todoing-rds-subnet-group"
  }
}

# RDS 인스턴스
resource "aws_db_instance" "todoing" {
  identifier        = "todoing-db"
  engine            = "mysql"
  engine_version    = "8.0"
  instance_class    = "db.t3.micro"
  allocated_storage = 20
  storage_type      = "gp2"

  db_name  = "todoing"
  username = "todoing_user"
  password = var.db_password

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.rds.name

  skip_final_snapshot = true
  publicly_accessible = true

  # Enhanced Monitoring(monitoring_interval > 0)은 프리티어 미포함 과금 대상이라 비활성화(기본값=0)

  tags = {
    Name = "todoing-db"
  }
}

# RDS 엔드포인트 출력
output "rds_endpoint" {
  value = aws_db_instance.todoing.endpoint
}

# CloudWatch 대시보드 생성
resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "todoing-dashboard"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          view    = "timeSeries"
          stacked = false
          metrics = [
            ["AWS/EC2", "CPUUtilization", "InstanceId", aws_instance.app.id]
          ]
          region = "ap-northeast-2"
          title  = "EC2 CPU 사용률 (%)"
          period = 300
          stat   = "Average"
          yAxis = {
            left = {
              min = 0
              max = 100
            }
          }
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          view    = "timeSeries"
          stacked = false
          metrics = [
            [
              "CWAgent",
              "mem_used_percent",
              "InstanceId", aws_instance.app.id,
              "InstanceType", aws_instance.app.instance_type
            ]
          ]
          region = "ap-northeast-2"
          title  = "EC2 메모리 사용률 (%)"
          period = 300
          stat   = "Average"
          yAxis = {
            left = {
              min = 0
              max = 100
            }
          }
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 12
        height = 6
        properties = {
          view    = "timeSeries"
          stacked = false
          metrics = [
            ["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", aws_db_instance.todoing.identifier]
          ]
          region = "ap-northeast-2"
          title  = "RDS CPU 사용률 (%)"
          period = 300
          stat   = "Average"
          yAxis = {
            left = {
              min = 0
              max = 100
            }
          }
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 6
        width  = 12
        height = 6
        properties = {
          view    = "timeSeries"
          stacked = false
          metrics = [
            ["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", aws_db_instance.todoing.identifier],
            ["AWS/RDS", "FreeableMemory", "DBInstanceIdentifier", aws_db_instance.todoing.identifier],
            ["AWS/RDS", "FreeStorageSpace", "DBInstanceIdentifier", aws_db_instance.todoing.identifier]
          ]
          region = "ap-northeast-2"
          title  = "RDS 상태"
          period = 300
          stat   = "Average"
        }
      }
    ]
  })
}

# EC2에 CloudWatch Agent 설치를 위한 IAM 정책 추가
resource "aws_iam_role_policy_attachment" "cloudwatch_agent" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}
package hongik.Todoing.domain.todo.label.repository;

import hongik.Todoing.domain.todo.label.domain.Label;
import hongik.Todoing.domain.todo.label.domain.LabelType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LabelRepository extends JpaRepository<Label, Long> {
    Label findByLabelId(Long labelId);

    Optional<Label> findByLabelName(LabelType labelName);
    boolean existsByLabelName(LabelType labelType);
}


// aggregated ( 도메인 집합 )
// 주문. --- 주문 아이템. -- 주문 옵션


// entity


// order
//. @Eager ,, fecth
// { prviate OrderItem items   }

// 주문 --- 주문아이템 갯수를 수정.







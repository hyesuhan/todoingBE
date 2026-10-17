package hongik.Todoing.domain.order.domain.pass;

import hongik.Todoing.domain.order.exception.passException.PassTypeMisMatchException;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import lombok.Getter;

@Getter
public enum ProductCode {
    PASS_1("PASS1", "1회 추가권", 1, 990),
    FREE_WEEKLY("FREEW", "주간 무료 이용권", 1, 0);

    private final String code;
    private final String name;
    private final int limitCount;
    private final int price;

    ProductCode(String code, String name, int limitCount, int price) {
        this.code = code;
        this.name = name;
        this.limitCount = limitCount;
        this.price = price;
    }

    public static ProductCode fromCode(String code) {
        for(ProductCode p : values()) {
            if(p.code.equalsIgnoreCase(code)) return p;
        }

        throw PassTypeMisMatchException.EXCEPTION;
    }

    // 자동 코드 생성 시 필요
    public static String getCodeIdByName(String name) {
        for(ProductCode p : values()) {
            if(p.name.equalsIgnoreCase(name)) {
                String code = p.code;
                if(code.length() >= 5) {
                    return "" + code.charAt(0) + code.charAt(4);
                }
                throw new GeneralException("코드의 형태가 잘못되었습니다.");
            }

        }

        throw PassTypeMisMatchException.EXCEPTION;
    }

}

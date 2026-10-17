package hongik.Todoing.domain.order.domain.pass;

import hongik.Todoing.domain.order.exception.passException.PassTypeMisMatchException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductCodeTest {

    @Test
    void fromCode_whenKnownCode_shouldReturnMatchingProductCode() {
        assertThat(ProductCode.fromCode("PASS1")).isEqualTo(ProductCode.PASS_1);
        assertThat(ProductCode.fromCode("FREEW")).isEqualTo(ProductCode.FREE_WEEKLY);
    }

    @Test
    void fromCode_whenUnknownCode_shouldThrowPassTypeMismatch() {
        assertThatThrownBy(() -> ProductCode.fromCode("UNKNOWN"))
                .isInstanceOf(PassTypeMisMatchException.class);
    }

    @Test
    void getCodeIdByName_shouldReturnFirstAndFifthCharOfCode() {
        assertThat(ProductCode.getCodeIdByName("1회 추가권")).isEqualTo("P1");
        assertThat(ProductCode.getCodeIdByName("주간 무료 이용권")).isEqualTo("FW");
    }
}

package com.expensetracker;

import com.expensetracker.util.MoneyUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Money math must be exact — paise are integers, so 0.10 + 0.20 == 0.30
 * with no floating-point error.
 */
class MoneyUtilTest {

    @Test
    void toPaise_convertsDecimalStrings() {
        assertThat(MoneyUtil.toPaise("1250.50")).isEqualTo(125050L);
        assertThat(MoneyUtil.toPaise("10")).isEqualTo(1000L);
        assertThat(MoneyUtil.toPaise("10.5")).isEqualTo(1050L);
        assertThat(MoneyUtil.toPaise("0.01")).isEqualTo(1L);
    }

    @Test
    void paiseArithmetic_hasNoFloatingPointError() {
        long sum = MoneyUtil.toPaise("0.10") + MoneyUtil.toPaise("0.20");
        assertThat(sum).isEqualTo(MoneyUtil.toPaise("0.30"));
        assertThat(MoneyUtil.toDecimalString(sum)).isEqualTo("0.30");
    }

    @Test
    void toDecimalString_formatsPaise() {
        assertThat(MoneyUtil.toDecimalString(125050L)).isEqualTo("1250.50");
        assertThat(MoneyUtil.toDecimalString(1000L)).isEqualTo("10.00");
        assertThat(MoneyUtil.toDecimalString(0L)).isEqualTo("0.00");
    }

    @Test
    void toPaise_rejectsInvalidInput() {
        assertThatThrownBy(() -> MoneyUtil.toPaise("abc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MoneyUtil.toPaise("10.999")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MoneyUtil.toPaise(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MoneyUtil.toPaise("")).isInstanceOf(IllegalArgumentException.class);
    }
}

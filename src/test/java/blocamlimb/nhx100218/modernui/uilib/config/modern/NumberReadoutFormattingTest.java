package blocamlimb.nhx100218.modernui.uilib.config.modern;

import org.junit.Assert;
import org.junit.Test;

import blocamlimb.nhx100218.modernui.config.ui.field.FieldRenderSupport;

public class NumberReadoutFormattingTest {

    @Test
    public void readoutTrimsFloatNoiseAndKeepsIntegersPlain() {
        Assert.assertEquals("1.333", FieldRenderSupport.formatReadout(1.3333333730697632D));
        Assert.assertEquals("0.3", FieldRenderSupport.formatReadout(0.1D + 0.2D));
        Assert.assertEquals("4", FieldRenderSupport.formatReadout(4.0D));
        Assert.assertEquals("0", FieldRenderSupport.formatReadout(-0.0D));
        Assert.assertEquals("1.3333333730697632",
                FieldRenderSupport.numberTextOf(1.3333333730697632D));
    }
}

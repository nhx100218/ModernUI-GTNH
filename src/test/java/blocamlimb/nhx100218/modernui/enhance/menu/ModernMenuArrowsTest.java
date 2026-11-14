package blocamlimb.nhx100218.modernui.enhance.menu;

import org.junit.Assert;
import org.junit.Test;

/** Hit-box contract for the resource-pack arrow layout. */
public class ModernMenuArrowsTest {

    @Test
    public void rightHalfIsJoin() {
        Assert.assertTrue(ModernMenuArrows.overRightHalf(24, 4, 0, 0, 32));
        Assert.assertFalse(ModernMenuArrows.overRightHalf(8, 4, 0, 0, 32));
    }

    @Test
    public void topLeftQuarterMovesUp() {
        Assert.assertTrue(ModernMenuArrows.overTopLeftQuarter(8, 8, 0, 0, 32));
        Assert.assertFalse(ModernMenuArrows.overTopLeftQuarter(24, 8, 0, 0, 32));
        Assert.assertFalse(ModernMenuArrows.overTopLeftQuarter(8, 24, 0, 0, 32));
    }

    @Test
    public void bottomLeftQuarterMovesDown() {
        Assert.assertTrue(ModernMenuArrows.overBottomLeftQuarter(8, 24, 0, 0, 32));
        Assert.assertFalse(ModernMenuArrows.overBottomLeftQuarter(8, 8, 0, 0, 32));
    }

    @Test
    public void iconHitBoxExcludesOutsidePixels() {
        Assert.assertTrue(ModernMenuArrows.overIcon(10, 20, 10, 20, 32));
        Assert.assertTrue(ModernMenuArrows.overIcon(41, 51, 10, 20, 32));
        Assert.assertFalse(ModernMenuArrows.overIcon(42, 20, 10, 20, 32));
        Assert.assertFalse(ModernMenuArrows.overIcon(9, 20, 10, 20, 32));
    }
}

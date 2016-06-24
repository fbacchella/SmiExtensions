package fr.jrds.SmiExtensions.types;

import org.junit.Assert;
import org.junit.Test;

import fr.jrds.SmiExtensions.types.Size;

public class SizeTest {
    
    @Test
    public void one() {
        Size s = new Size("4");
        Assert.assertEquals(null, s.extract(new int[]{1, 2, 3}));
        Assert.assertEquals(4, s.extract(new int[]{1, 2, 3, 4}).length);
        Assert.assertEquals(4, s.extract(new int[]{1, 2, 3, 4, 5}).length);
    }

    @Test
    public void two() {
        Size s = new Size("4 | 8");
        Assert.assertEquals(4, s.extract(new int[]{1, 2, 3, 4}).length);
        Assert.assertEquals(4, s.extract(new int[]{1, 2, 3, 4, 5}).length);
        Assert.assertEquals(8, s.extract(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}).length);
    }

    @Test
    public void three() {
        Size s = new Size("4..8");
        Assert.assertEquals(4, s.extract(new int[]{1, 2, 3, 4}).length);
        Assert.assertEquals(5, s.extract(new int[]{1, 2, 3, 4, 5}).length);
        Assert.assertEquals(8, s.extract(new int[]{1, 2, 3, 4, 5, 6, 7, 8}).length);
        Assert.assertEquals(8, s.extract(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}).length);
    }
}

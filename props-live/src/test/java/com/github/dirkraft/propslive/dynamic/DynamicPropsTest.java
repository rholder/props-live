package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.propsrc.PropertySourceMap;
import junit.framework.Assert;
import org.junit.Test;

import javax.xml.ws.Holder;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class DynamicPropsTest {

    DynamicProps $ = new DynamicProps(new PropertySourceMap(DynamicPropsTest.class.getName()));

    Holder<Integer> triggeredReload = new Holder<>(0);
    DynamicPropListener<Boolean> listener = new DynamicPropListener<Boolean>() {
        @Override
        public void reload(PropChange<Boolean> values) {
            ++triggeredReload.value;
        }
    };

    @Test
    public void testNoRegistration() {
        Boolean bool = $.getBool("test.bool");
        Assert.assertNull(bool);
        Assert.assertEquals(0, triggeredReload.value.intValue());

        $.setBool("test.bool", false);
        bool = $.getBool("test.bool");
        Assert.assertFalse(bool);
        Assert.assertEquals(0, triggeredReload.value.intValue());
    }

    @Test
    public void testRegistration() {
        $.setBool("test.bool", false);

        Boolean bool = $.to(listener).getBool("test.bool");
        Assert.assertFalse(bool);
        Assert.assertEquals(0, triggeredReload.value.intValue());

        bool = $.to(listener).getBool("test.bool");
        Assert.assertFalse(bool);
        Assert.assertEquals("now that it's registered make sure any get doesn't trigger reload",
                0, triggeredReload.value.intValue());
        bool = $.getBool("test.bool");
        Assert.assertFalse(bool);
        Assert.assertEquals(0, triggeredReload.value.intValue());

        $.setBool("test.bool", false);
        Assert.assertEquals("setting to same as previous should not trigger reload",
                0, triggeredReload.value.intValue());
        $.setBool("test.bool", true);
        Assert.assertEquals("setting to a different value should trigger reload",
                1, triggeredReload.value.intValue());

        $.setBool("another.bool", false);
        Assert.assertEquals("setting some other thing to a different value should NOT trigger reload",
                1, triggeredReload.value.intValue());
    }

}

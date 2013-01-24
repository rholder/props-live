package com.github.dirkraft.propslive.set;

import com.github.dirkraft.propslive.propsrc.PropSourceMap;
import com.github.dirkraft.propslive.set.ease.PropSetAsMap;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropSetMapTest {

    PropsSetsImpl impl = new PropsSetsImpl(new PropSourceMap(getClass().getName()));
    PropSetAsMap propSet = new PropSetAsMap("test.one", "test.two", "test.three");

    @Test
    public void testGet() {
        Assert.assertNull(impl.getString("test.one"));
        Assert.assertNull(impl.getString("test.two"));
        Assert.assertNull(impl.getString("test.three"));

        Map<String,String> vals = impl.getVals(propSet);
        Assert.assertEquals(3, vals.size());
        Assert.assertNull(vals.get("test.one"));
        Assert.assertNull(vals.get("test.two"));
        Assert.assertNull(vals.get("test.three"));

        impl.setString("test.one", "1");
        impl.setString("test.two", "2");
        // skip three
        vals = impl.getVals(propSet);
        Assert.assertEquals(3, vals.size());
        Assert.assertEquals("1", vals.get("test.one"));
        Assert.assertEquals("2", vals.get("test.two"));
        Assert.assertNull(vals.get("test.three"));
    }

    @Test
    public void testDefaults() {
        Assert.assertNull(impl.getString("test.one"));
        Assert.assertNull(impl.getString("test.two"));
        Assert.assertNull(impl.getString("test.three"));

        Map<String,String> vals = impl.getVals(propSet);
        Assert.assertEquals(3, vals.size());
        Assert.assertNull(vals.get("test.one"));
        Assert.assertNull(vals.get("test.two"));
        Assert.assertNull(vals.get("test.three"));

        impl.setString("test.three", "3");
        propSet.withDefaults("ONE", "TWO", "THREE");

        vals = impl.getVals(propSet);
        Assert.assertEquals(3, vals.size());
        Assert.assertEquals("ONE", vals.get("test.one"));
        Assert.assertEquals("TWO", vals.get("test.two"));
        Assert.assertEquals("3", vals.get("test.three"));
    }

    @Test
    public void testWrite() {
        Assert.assertNull(impl.getString("test.one"));
        Assert.assertNull(impl.getString("test.two"));
        Assert.assertNull(impl.getString("test.three"));

        Map<String,String> vals = impl.getVals(propSet);
        Assert.assertEquals(3, vals.size());
        Assert.assertNull(vals.get("test.one"));
        Assert.assertNull(vals.get("test.two"));
        Assert.assertNull(vals.get("test.three"));

        propSet.withWrites("1", PropSetAsMap.SKIP_WRITE, "3");
        impl.setVals(propSet);

        vals = impl.getVals(propSet.withDefaults("defaulted", "defaulted", "defaulted"));
        Assert.assertEquals("1", vals.get("test.one"));
        Assert.assertEquals("defaulted", vals.get("test.two"));
        Assert.assertEquals("3", vals.get("test.three"));

        Assert.assertEquals("1", impl.getString("test.one"));
        Assert.assertEquals(null, impl.getString("test.two"));
        Assert.assertEquals("3", impl.getString("test.three"));
    }
}

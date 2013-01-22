package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.propsrc.PropertySourceMap;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class DynamicProps_PropSetTests {

    DynamicProps $ = new DynamicProps(new PropertySourceMap(DynamicProps_PropSetTests.class.getName()));

    @Test
    public void testPropSets() {
        final AtomicInteger counter = new AtomicInteger();

        PropSet<Integer> singleIntPropSet = new PropSet<Integer>() {
            @Override
            public LinkedHashSet<String> propKeys() {
                return new LinkedHashSet<>(Arrays.asList("test.key"));
            }

            @Override
            public Integer getVals(Props props) {
                return props.getInt("test.key");
            }

            @Override
            public void setVals(Props props) {
                props.setInt("test.key", counter.getAndIncrement());
            }
        };

        Integer val = $.getInt("test.key");
        Assert.assertEquals(null, val);

        val = $.getPropSet(singleIntPropSet);
        Assert.assertEquals(null, val);

        $.setPropSet(singleIntPropSet);
        val = $.getPropSet(singleIntPropSet);
        Assert.assertEquals(0, val.intValue());
    }

}

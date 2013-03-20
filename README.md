Props Live /präps līv/
=======================
A utility for easing reading and writing properties, with support for subscribing to property change events. Every
attempt has been made for maximum concurrent reading of properties simultaneous with up to 1 writer.

### Maven

    <dependency>
      <groupId>com.github.dirkraft</groupId>
      <artifactId>props-live</artifactId>
      <version>1.0.0</version>
    </dependency>

### Gradle

    compile 'com.github.dirkraft:props-live:1.0.0'

----

## Usage
I like to have a singleton or static instance somewhere with a very short name.

    public class MyConfig {
        public static final PropsLive $ = new PropsLive();
    }


----

## Supported Scenarios

1. You need to parse all matter of simple types from some property source (e.g. system properties), Strings, Booleans,
   Integers and others.

       point to sample

2. You have an application that could actually be reconfigured **while it is running**.

       point to sample

3. You have a management interface that can change configuration on the fly. You expect that for each property changed
   any application component dependent on it, would reinitialize as necessary.

       point to sample



Documentation
-------------
Does anyone really want javadoc? A considerable effort has been made to javadoc the code. The maven sources jar should
be picked up by your tooling.



License
-------
`props-live` is released under the [Apache 2 license](http://www.apache.org/licenses/LICENSE-2.0)



Contributors
------------

 * Jason Dunkelberger (dirkraft)
 * and so can you


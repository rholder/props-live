Props Live /laɪv/
=================
Any good project starts out with system properties. This package provides a few conveniences for accessing said
properties. More importantly, it was written with live-configuration changes in mind, and so discourages caching
property values anywhere and instead always looking them up. There are mechanisms for grouping multiple props together
into a logically atomic set, such that the set of properties may be atomically updated, and listeners atomically
notified. There are versions of every property access where one can pass itself to automatically register itself
a listener against change events on those properties.
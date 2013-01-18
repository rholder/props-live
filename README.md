System Properties Live /laɪv/
=============================
Any good project starts out with system properties. This package provides a few conveniences for accessing said properties. It was written with live-configuration changes in mind, and so discourages caching property values anywhere and instead always looking them up. There are mechanisms for grouping multiple props together into a logically atomic set, such that the set of properties may be atomically updated, and listeners atomically notified.
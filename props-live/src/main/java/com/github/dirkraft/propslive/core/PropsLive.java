package com.github.dirkraft.propslive.core;

import com.github.dirkraft.propslive.dynamic.DynamicPropsSets;

/**
 * Alias for DynamicPropsSets to better represent the raison d'être of this java package. e.g. Create one PropsLive
 * instance in your application. All components may define instances/subclasses of {@link LivePropSet} to atomically
 * read/write singular or multiple properties from the singular PropsLive instance, and optionally subscribe to change
 * events on them.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropsLive extends DynamicPropsSets {
}

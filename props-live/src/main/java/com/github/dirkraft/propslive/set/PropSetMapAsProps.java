package com.github.dirkraft.propslive.set;

import com.github.dirkraft.propslive.Props;

/**
 * Extension of {@link PropSetMap} except that {@link #getVals(Props)} returns a {@link Props} to enable the convenience
 * of the different type parsers
 *
 * TODO jason, need to refactor PropSetMap into abstract and two concrete (Map<String, String> and Props)
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropSetMapAsProps extends PropSetMap {
}

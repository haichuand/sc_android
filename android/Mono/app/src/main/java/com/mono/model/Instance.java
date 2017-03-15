package com.mono.model;

import com.mono.util.Common;

/**
 * This data structure is used to represent an instance consisting of an identifier.
 *
 * @author Gary Ng
 */
public abstract class Instance {

    public String id;

    public Instance(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Instance)) {
            return false;
        }

        Instance instance = (Instance) object;

        if (!Common.compareStrings(id, instance.id)) {
            return false;
        }

        return true;
    }
}

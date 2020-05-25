package com.teraim.fieldapp.synchronization;

import java.util.Set;

public class Unikey {
    private final String rep;
    private final String uid;
    private final String spy;
    public Unikey(String uuid, String spy) {
        uid=uuid;
        this.spy=spy;
        rep = generate(uuid,spy);
    }

    public String getUid() {
        return uid;
    }

    public String getSpy() {
        return spy;
    }
    private String getKey() {
        return rep;
    }

    @Override
    public String toString() {
        return rep;
    }

    private static String generate(String uid, String spy) {
        if (spy!=null) {
            return uid+"|"+spy;
        } else
            return uid;
    }
    public static Unikey FindKeyFromParts(String uid,String spy, Set<Unikey> lst) {
        if (lst==null || uid==null)
            return null;
        String uKey = generate(uid,spy);
        for (Unikey key:lst){
            if (key.getKey().equals(uKey))
                return key;
        }
        return null;
    }

}

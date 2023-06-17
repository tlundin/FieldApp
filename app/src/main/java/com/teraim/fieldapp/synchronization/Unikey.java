package com.teraim.fieldapp.synchronization;

import java.util.Set;

public class Unikey {
    private final String rep;
    private final String uid;
    private final String sub;
    public Unikey(String uuid, String sub) {
        uid=uuid;
        this.sub=sub;
        rep = generate(uuid,sub);
    }

    public String getUid() {
        return uid;
    }
    public String getSub() {
        return sub;
    }
    private String getKey() {
        return rep;
    }

    @Override
    public String toString() {
        return rep;
    }

    private static String generate(String uid, String sub) {
        String key=uid;
        if (sub!=null)
            key = key+"|"+sub;
        return key;
    }
    public static Unikey FindKeyFromParts(String uid,String sub, Set<Unikey> lst) {
        if (lst==null || uid==null)
            return null;
        String uKey = generate(uid,sub);
        for (Unikey key:lst){
            if (key.getKey().equals(uKey))
                return key;
        }
        return null;
    }

}

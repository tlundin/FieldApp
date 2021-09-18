package com.teraim.fieldapp.synchronization;

import java.util.Set;

public class Unikey {
    private final String rep;
    private final String uid;
    private final String spy;
    private final String vps;
    public Unikey(String uuid, String spy, String vps) {
        uid=uuid;
        this.spy=spy;
        this.vps=vps;
        rep = generate(uuid,spy,vps);
    }

    public String getUid() {
        return uid;
    }
    public String getSpy() {
        return spy;
    }
    public String getVps() {
        return vps;
    }
    private String getKey() {
        return rep;
    }

    @Override
    public String toString() {
        return rep;
    }

    private static String generate(String uid, String spy, String vps) {
        String key=uid;
        if (spy!=null)
            key = key+"|"+spy;
        if (vps != null)
            key = key+"|"+vps;
            return key;
    }
    public static Unikey FindKeyFromParts(String uid,String spy,String vps, Set<Unikey> lst) {
        if (lst==null || uid==null)
            return null;
        String uKey = generate(uid,spy,vps);
        for (Unikey key:lst){
            if (key.getKey().equals(uKey))
                return key;
        }
        return null;
    }

}

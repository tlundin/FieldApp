package com.teraim.fieldapp.dynamic.types;

import com.teraim.fieldapp.non_generics.DelyteManager.Coord;

public record Segment(Coord start, Coord end, boolean isArc) {
    public boolean within(Segment s) {
        if (s.start.rikt > s.end.rikt) {
            return ((start.rikt >= s.start.rikt || start.rikt <= s.end.rikt) &&
                    (end.rikt <= s.end.rikt || end.rikt >= s.start.rikt));

        } else {

            return ((start.rikt <= s.start.rikt || start.rikt >= s.end.rikt) &&
                    (end.rikt >= s.end.rikt || end.rikt <= s.start.rikt));
        }


    }

}

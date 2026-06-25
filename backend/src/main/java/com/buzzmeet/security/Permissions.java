package com.buzzmeet.security;

public final class Permissions {

    public static final String MEETING_CREATE = "meeting:create";
    public static final String MEETING_VIEW = "meeting:view";
    public static final String MEETING_BOOK = "meeting:book";
    public static final String MEETING_PARTICIPANTS_UPDATE = "meeting:participants:update";
    public static final String MEETING_OVERRIDE = "meeting:override";
    public static final String USER_MANAGE = "user:manage";
    public static final String ROOM_MANAGE = "room:manage";
    public static final String EQUIPMENT_MANAGE = "equipment:manage";

    private Permissions() {
    }
}
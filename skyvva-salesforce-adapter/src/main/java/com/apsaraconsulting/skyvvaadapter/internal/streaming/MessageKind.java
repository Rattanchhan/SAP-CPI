package com.apsaraconsulting.skyvvaadapter.internal.streaming;

public enum MessageKind {

    PLATFORM_EVENT("/event/"),
    PUSH_TOPIC("/topic/"),
    CDC_EVENT("/data/");

    private String channelPrefix;

    MessageKind(String channelPrefix) {
        this.channelPrefix = channelPrefix;
    }

    public static MessageKind fromTopicName(final String topicName) {
        if (topicName.startsWith("event/") || topicName.startsWith("/event/")) {
            return MessageKind.PLATFORM_EVENT;
        } else if (topicName.startsWith("data/") || topicName.startsWith("/data/") || topicName.endsWith("ChangeEvent")) {
            return MessageKind.CDC_EVENT;
        }

        return PUSH_TOPIC;
    }

    public static String resolveChannelName(final String topicName) {
        return resolveChannelName(topicName, fromTopicName(topicName));
    }

    public static String resolveChannelName(String topicName, final MessageKind messageKind) {

        final StringBuilder channelName = new StringBuilder("/");
        if (topicName.startsWith("/")) {
            topicName = topicName.substring(1);
        }

        if (topicName.indexOf('/') > -1) {
            channelName.append(topicName);
        } else if (messageKind.equals(MessageKind.PUSH_TOPIC)){
            channelName.append("topic/");
            channelName.append(topicName);
        } else if (messageKind.equals(MessageKind.CDC_EVENT)) {
            channelName.append("data/");
            channelName.append(topicName);
        }

        if (messageKind.equals(MessageKind.PLATFORM_EVENT) && !topicName.endsWith("__e")) {
            channelName.append("__e");
        }

        return channelName.toString();
    }
}

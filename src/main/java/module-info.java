module org.redlance.common {
    // Adventure
    requires net.kyori.adventure;
    requires net.kyori.adventure.key;
    requires net.kyori.examination.api;
    requires net.kyori.adventure.text.serializer.gson;
    requires net.kyori.adventure.text.serializer.legacy;

    // Emotecraft
    requires emotecraftApi;
    requires emotecraftAssets;
    requires emotecraftServer;
    requires playerAnimator;

    // Requester
    requires methanol;
    requires methanol.adapter.gson;
    requires webGrude;

    // Logging
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.jul;

    requires org.jetbrains.annotations;
    requires org.apache.commons.io;
    requires com.google.gson;

    // Java
    requires java.desktop;
    requires jdk.unsupported;
    requires java.logging;
}
module io.github.mzmine {
    // Swing
    requires java.desktop;
    // SQL
    requires java.sql;
    // JavaFX
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.swing;
    requires javafx.controls;
    requires javafx.web;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    // Logging
    requires org.slf4j;
    // Preferences and XML
    requires java.prefs;
    requires java.xml.bind;
    requires jdk.xml.dom;

    // Other modules
    requires commons.logging;
    requires com.google.common;

    requires java.scripting;
    requires java.security.jgss;
    requires java.security.sasl;
    requires java.rmi;
    requires java.logging;
    requires java.datatransfer;
    requires java.management;
    requires java.xml;
    requires java.xml.crypto;
    requires java.naming;
    requires jdk.internal.vm.compiler;
    requires jsr305;
    requires jdk.jfr;
    requires jdk.unsupported.desktop;
    requires jdk.unsupported;
    requires jdk.jartool;
    requires jdk.naming.rmi;
    requires jdk.jsobject;
    requires org.apache.commons.lang3;
    requires jfreechart;
    requires xmlgraphics.commons;
    requires freehep.graphicsio.emf;
    requires cdk.core;
    requires javax.mail.api;
    requires itextpdf;
    requires batik.anim;
    requires freehep.graphics2d;
    requires batik.svggen;
    requires epsgraphics;
    requires REngine;
    requires Rserve;
    requires RCaller;
    requires java.compiler;
    requires commons.math3;
    requires commons.math;
    requires poi.ooxml;
    requires poi;
    requires commons.io;
    requires org.jfree.fxgraphics2d;
    requires gs.core;
    requires jfreechart.fx;
    requires fontchooser;
    requires miglayout;
    requires utils;
//    requires jmol;
    requires it.unimi.dsi.fastutil;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient;
    requires httpmime;
    requires cdm;
    requires jmzml;
    requires jmztab.modular.model;
    requires jmztab.modular.util;
    requires org.json;
    requires java.json;
    requires weka.stable;
    requires msdk.datamodel;
    requires adap;
//    requires clustering;
    requires chemspider.api;
    requires jmprojection;
    requires msdk.featuredetection.adap3d;

}

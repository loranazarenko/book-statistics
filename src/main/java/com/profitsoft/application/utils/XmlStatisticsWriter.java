package com.profitsoft.application.utils;

import com.profitsoft.application.entities.StatisticsItem;
import lombok.extern.slf4j.Slf4j;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Writer for generating XML statistics files.
 */
@Slf4j
public class XmlStatisticsWriter {

    public void writeStatistics(Path outPath, List<StatisticsItem> stats) throws IOException, XMLStreamException {
        if (stats == null) stats = Collections.emptyList();
        Path parent = outPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(outPath))) {
            XMLStreamWriter writer = factory.createXMLStreamWriter(os, "UTF-8");
            try {
                writer.writeStartDocument("UTF-8", "1.0");
                writer.writeStartElement("statistics");

                for (StatisticsItem item : stats) {
                    writer.writeStartElement("item");

                    writer.writeStartElement("value");
                    String value = item.getValue() == null ? "" : item.getValue();
                    writer.writeCharacters(value);
                    writer.writeEndElement();

                    writer.writeStartElement("count");
                    writer.writeCharacters(String.valueOf(item.getCount()));
                    writer.writeEndElement();

                    writer.writeEndElement();
                }

                writer.writeEndElement();
                writer.writeEndDocument();
            } finally {
                try {
                    writer.close();
                } catch (XMLStreamException e) {
                    log.error("Error closing XML writer: {}", e.getMessage());
                }
            }
        } catch (IOException | XMLStreamException e) {
            log.error("Failed to write XML to {}: {}", outPath, e.getMessage());
            throw e;
        }
    }
}
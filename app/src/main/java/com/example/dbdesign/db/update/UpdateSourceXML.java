package com.example.dbdesign.db.update;

import android.text.TextUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class UpdateSourceXML implements IUpdateSource {

    private List<CreateVersion> createList;
    private List<UpdateStep> updateList;
    private String latestVersion;

    public UpdateSourceXML(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            init(file);
        }
    }

    public void init(File file) {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            try {
                Document document = documentBuilder.parse(file);
                NodeList nodeList = document.getElementsByTagName("updateXml");
                Element rootElement = (Element) nodeList.item(0);
                latestVersion = rootElement.getAttribute("version");
                createList = getTagList(rootElement, "createVersion", CreateVersion.class);
                updateList = getTagList(rootElement, "updateStep", UpdateStep.class);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }


    private <T> List<T> getTagList(Element parentElement, String tagName, Class<T> tagClass) {
        NodeList nodeList = parentElement.getElementsByTagName(tagName);
        int length = nodeList.getLength();
        List<T> objList = null;
        if (length > 0) {
            objList = new ArrayList<>();
        }
        for (int i = 0; i < length; i++) {
            Element tagElement = (Element) nodeList.item(i);
            if (tagClass == UpdateDB.class) {
                objList.add((T) getUpdateDB(tagElement));
            } else if (tagClass == CreateDB.class) {
                objList.add((T) getCreateDB(tagElement));
            } else if (tagClass == UpdateStep.class) {
                objList.add((T) getUpdateStep(tagElement));
            }else if (tagClass == CreateVersion.class) {
                objList.add((T) getCreateVersion(tagElement));
            }
        }
        return objList;
    }

    private UpdateStep getUpdateStep(Element element) {
        List<UpdateDB> dbList = getTagList(element, "updateDb", UpdateDB.class);
        UpdateStep updateStep = new UpdateStep(element.getAttribute("versionFrom"),
                element.getAttribute("versionTo"), dbList);
        return updateStep;
    }

    private CreateVersion getCreateVersion(Element element) {
        List<CreateDB> dbList = getTagList(element, "createDb", CreateDB.class);
        CreateVersion createVersion = new CreateVersion(element.getAttribute("version"),
                dbList);
        return createVersion;
    }

    private UpdateDB getUpdateDB(Element element) {
        String dbName = element.getAttribute("name");

        //node updatedb
        NodeList beforeNodeList = element.getElementsByTagName("sql_before");
        List<String> beforeList = getTextContents(beforeNodeList);

        NodeList afterNodeList = element.getElementsByTagName("sql_after");
        List<String> afterList = getTextContents(afterNodeList);

        UpdateDB updateDB = new UpdateDB(dbName, beforeList, afterList);
        return updateDB;
    }

    private CreateDB getCreateDB(Element element) {
        String dbName = element.getAttribute("name");

        //node updatedb
        NodeList createNodeList = element.getElementsByTagName("sql_createTable");
        List<String> createList = getTextContents(createNodeList);

        CreateDB createDB = new CreateDB(dbName, createList);
        return createDB;
    }

    private List<String> getTextContents(NodeList nodeList) {
        int length = nodeList.getLength();
        List<String> list = null;
        if (length > 0) {
            list = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                Element childElement = (Element) nodeList.item(i);
                String content = childElement.getTextContent();
                if (TextUtils.isEmpty(content)) {
                    continue;
                }
                list.add(content);
            }
            if (list.size() == 0) {
                list = null;
            }
        }
        return list;
    }

    @Override
    public List<CreateVersion> getCreateVersions() {
        return createList;
    }

    @Override
    public List<UpdateStep> getUpdateStep() {
        return updateList;
    }

    @Override
    public String getLatestVersion() {
        return latestVersion;
    }
}


package com.api.util;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.api.item.dto.ItemDetail;

/**
 * 온비드 XML 응답을 ItemDetail DTO 리스트로 변환하는 유틸리티.
 */
public final class ApiXmlParser {

    private static final Logger log = LoggerFactory.getLogger(ApiXmlParser.class);
    private static final DateTimeFormatter[] DATE_TIME_FORMATS = new DateTimeFormatter[] {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.KOREA),
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.KOREA)
    };
    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[] {
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.KOREA),
        DateTimeFormatter.ofPattern("yyyyMMdd", Locale.KOREA)
    };

    private ApiXmlParser() {
    }

    public static List<ItemDetail> parseNewItemDetails(String xml) {
        if (xml == null || xml.isBlank()) {
            log.warn("⚠️ XML 응답이 null이거나 비어있음");
            return List.of();
        }

        // API 에러 응답 확인
        if (xml.contains("<resultCode>") || xml.contains("<resultMsg>") || xml.contains("<error>")) {
            log.warn("⚠️ API 에러 응답 감지: {}", xml.length() > 500 ? xml.substring(0, 500) : xml);
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));

            // XML 구조 확인을 위한 로깅
            String rootElementName = document.getDocumentElement() != null ? document.getDocumentElement().getNodeName() : "unknown";
            log.debug("📄 XML 루트 요소: {}", rootElementName);

            NodeList nodes = document.getElementsByTagName("item");
            log.debug("📄 XML에서 찾은 item 개수: {}", nodes.getLength());
            
            if (nodes.getLength() == 0) {
                // 다른 태그명으로 시도
                NodeList bodyNodes = document.getElementsByTagName("body");
                if (bodyNodes.getLength() > 0) {
                    Element body = (Element) bodyNodes.item(0);
                    nodes = body.getElementsByTagName("item");
                    log.debug("📄 body 내부에서 찾은 item 개수: {}", nodes.getLength());
                }
            }
            
            List<ItemDetail> results = new ArrayList<>(nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element element = (Element) node;
                ItemDetail detail = readItemDetail(element);
                if (detail.getPlnmNo() != null) {
                    results.add(detail);
                }
            }
            
            log.info("✅ XML 파싱 완료: 총 {}개 중 {}개 파싱 성공", nodes.getLength(), results.size());
            return results;
        } catch (Exception ex) {
            log.error("❌ Onbid XML 응답 파싱 실패: {}", ex.getMessage(), ex);
            // XML의 일부를 로깅하여 디버깅에 도움
            if (xml.length() > 500) {
                log.error("❌ XML 응답 일부 (처음 500자): {}", xml.substring(0, 500));
            } else {
                log.error("❌ XML 응답 전체: {}", xml);
            }
            return List.of();
        }
    }

    private static ItemDetail readItemDetail(Element element) {
        return ItemDetail.builder()
            .rnum(getInteger(element, "RNUM"))
            .plnmNo(getLong(element, "PLNM_NO"))
            .address(firstNonBlank(
                getText(element, "LDNM_ADRS"),
                getText(element, "NMRD_ADRS"),
                getText(element, "ROAD_ADDR")
            ))
            .appraisalAmountMin(getLong(element, "APSL_ASES_AVG_AMT"))
            .appraisalAmountMax(getLong(element, "APSL_ASES_AVG_AMT"))
            .minBidPriceMin(getLong(element, "MIN_BID_PRC"))
            .minBidPriceMax(getLong(element, "MIN_BID_PRC"))
            .orgName(getText(element, "ORG_NM"))
            .bidStart(parseDateTime(element, "PBCT_BEGN_DTM"))
            .bidEnd(parseDateTime(element, "PBCT_CLS_DTM"))
            .disposalMethod(getText(element, "DPSL_MTD_NM"))
            .bidMethod(getText(element, "BID_MTD_NM"))
            .pbctNo(getLong(element, "PBCT_NO"))
            .orgBaseNo(getLong(element, "ORG_BASE_NO"))
            .cltrMnmtNo(getText(element, "CLTR_MNMT_NO"))
            .nmrAddress(getText(element, "LDNM_ADRS"))
            .roadName(firstNonBlank(
                getText(element, "NMRD_ADRS"),
                getText(element, "ROD_NM")
            ))
            .bldNo(getText(element, "BLD_NO"))
            .bidStatus(getText(element, "PBCT_CLTR_STAT_NM"))
            .viewCount(getInteger(element, "IQRY_CNT"))
            .goodsDetail(getText(element, "GOODS_NM"))
            .assetCategory(getText(element, "CTGR_FULL_NM"))
            .bidRoundNo(getText(element, "BID_MNMT_NO"))
            .feeRate(getText(element, "FEE_RATE"))
            .jointBid(parseBoolean(element, "JOINT_BID"))
            .electronicGuarantee(parseBoolean(element, "ELEC_GUAR"))
            .agentBid(parseBoolean(element, "AGENT_BID"))
            .build();
    }

    private static Boolean parseBoolean(Element element, String tagName) {
        String value = getText(element, tagName);
        if (value == null) {
            return null;
        }
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        if ("Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("N".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static LocalDateTime parseDateTime(Element element, String tagName) {
        String raw = getText(element, tagName);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim();
        for (DateTimeFormatter formatter : DATE_TIME_FORMATS) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (Exception ignore) {
                // next format
            }
        }

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                LocalDate date = LocalDate.parse(normalized, formatter);
                return date.atStartOfDay();
            } catch (Exception ignore) {
                // next format
            }
        }
        return null;
    }

    private static Integer getInteger(Element element, String tagName) {
        Long value = getLong(element, tagName);
        return value == null ? null : value.intValue();
    }

    private static Long getLong(Element element, String tagName) {
        String text = getText(element, tagName);
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.replaceAll("[^0-9\\-]", "");
        if (normalized.isBlank() || "-".equals(normalized)) {
            return null;
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ex) {
            log.debug("Failed to parse long value '{}' for tag {}", text, tagName);
            return null;
        }
    }

    private static String getText(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) {
            return null;
        }
        Node node = nodeList.item(0);
        if (node == null || node.getTextContent() == null) {
            return null;
        }
        String text = node.getTextContent().trim();
        return text.isEmpty() ? null : text;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}



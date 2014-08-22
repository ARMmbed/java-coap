/**
 * Copyright (C) 2011-2014 ARM Limited. All rights reserved.
 */
package org.mbed.coap.linkformat;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author szymon
 */
public class LinkFormatBuilder {

    public static String toString(Collection<LinkFormat> links) {
        StringBuilder sb = new StringBuilder();
        for (LinkFormat lf : links) {
            lf.toString(sb);
            sb.append(',');
        }
        //remove last unnecessary comma
        if (sb.length() > 2) {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * Parses link value list.
     *
     * @param linkValueList textual representation of link value list
     * @return array of link format
     * @throws ParseException
     */
    public static LinkFormat[] parseList(String linkValueList) throws ParseException {
        List<LinkFormat> linkFormats = parseLinkAsList(linkValueList);
        return linkFormats.toArray(new LinkFormat[linkFormats.size()]);
    }

    /**
     * Parses link value list.
     *
     * @param linkValueList textual representation of link value list
     * @return list with parsed link value objects
     * @throws ParseException
     */
    public static List<LinkFormat> parseLinkAsList(String linkValueList) throws ParseException {
        List<LinkFormat> linkHeaderList = new LinkedList<LinkFormat>();
        for (String ln : linkValueList.split(",")) {
            LinkFormat lf = parse(ln);
            linkHeaderList.add(lf);
        }
        return linkHeaderList;
    }

    /**
     * Parses single line of link value
     *
     * @param ln link value text representation
     * @return LinkFormat parsed object
     * @throws ParseException
     */
    public static LinkFormat parse(String ln) throws ParseException {
        String[] subLn = ln.split(";");
        if (subLn.length <= 0) {
            return null;
        }
        if (subLn[0].indexOf('<') < 0 || subLn[0].indexOf('>') < 0) {
            throw new ParseException("Can not parse URI-Reference", 0);
        }
        LinkFormat lf = new LinkFormat();
        try {
            lf.uri = subLn[0].substring(subLn[0].indexOf('<') + 1, subLn[0].indexOf('>'));
        } catch (StringIndexOutOfBoundsException ex) {
            throw new ParseException("Can not parse URI-Reference", 0); //NOPMD
        }

        for (int i = 1; i < subLn.length; i++) {
            int divider = subLn[i].indexOf('=');
            if (divider >= 0) {
                LinkFormat.parseParam(lf, subLn[i].substring(0, divider), subLn[i].substring(divider + 1));
            } else {
                LinkFormat.parseParam(lf, subLn[i], null);
            }

        }
        return lf;
    }

    public static List<LinkFormat> filter(List<LinkFormat> list, Map<String, String> queryFilter) {
        if (queryFilter == null || queryFilter.isEmpty()) {
            return list;
        }

        List<LinkFormat> filteredList = new ArrayList<LinkFormat>();
        for (LinkFormat lf : list) {
            boolean isAccepted = true;
            for (Map.Entry<String, String> entry : queryFilter.entrySet()) {
                String val = entry.getValue();
                String key = entry.getKey();
                isAccepted = filter(key, lf, isAccepted, val);
                if (!isAccepted) {
                    break;
                }
            }
            if (isAccepted) {
                filteredList.add(lf);
            }
        }

        return filteredList;
    }

    private static boolean filter(String key, LinkFormat lf, boolean isAccepted, String val) {
        if (key.equals(LinkFormat.LINK_RELATIONS) || key.equals(LinkFormat.LINK_REV) || key.equals(LinkFormat.LINK_RESOURCE_TYPE)
                || key.equals(LinkFormat.LINK_INTERFACE_DESCRIPTION) || key.equals(LinkFormat.LINK_CONTENT_TYPE)) {
            //for parameters with multiple values ('relation-types')
            String[] paramVals = lf.getParamRelationTypes(key);
            isAccepted &= hasMatch(val, paramVals);
        } else if (key.equals(LinkFormat.LINK_OBSERVABLE) || key.equals(LinkFormat.LINK_EXPORT)) {
            //for flag parameters
            isAccepted &= lf.getParam(key) != null;
        } else if ("href".equals(key)) {
            isAccepted &= hasMatch(val, lf.getUri());
        } else {
            isAccepted &= hasMatch(val, lf.getParam(key));
        }
        return isAccepted;
    }

    private static boolean hasMatch(String queryVal, String... paramVals) {
        if (paramVals == null || paramVals.length == 0 || paramVals[0] == null) {
            return false;
        }
        if (queryVal.endsWith("*")) {
            String query = queryVal.substring(0, queryVal.length() - 1);
            for (String p : paramVals) {
                if (p.startsWith(query)) {
                    return true;
                }
            }
        } else {
            for (String p : paramVals) {
                if (p.equals(queryVal)) {
                    return true;
                }
            }
        }

        return false;
    }
}
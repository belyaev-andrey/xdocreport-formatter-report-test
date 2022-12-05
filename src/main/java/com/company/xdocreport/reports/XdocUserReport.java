package com.company.xdocreport.reports;

import com.haulmont.yarg.formatters.CustomReport;
import com.haulmont.yarg.structure.BandData;
import com.haulmont.yarg.structure.Report;
import com.haulmont.yarg.structure.ReportTemplate;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XdocUserReport implements CustomReport {

    @Override
    public byte[] createReport(Report report, BandData rootBand, Map<String, Object> params) {
        try {
            ReportTemplate reportTemplate = report.getReportTemplates().get("DEFAULT");
            InputStream inputStream = reportTemplate.getDocumentContent();
            IXDocReport docReport = XDocReportRegistry.getRegistry().loadReport(inputStream, TemplateEngineKind.Velocity);

            IContext ctx = docReport.createContext();
            FieldsMetadata metadata = docReport.createFieldsMetadata();

            Set<Map.Entry<String, List<BandData>>> childrenBands = rootBand.getChildrenBands().entrySet();

            for (Map.Entry<String, List<BandData>> entry : childrenBands) {
                String key = entry.getKey();
                if (entry.getValue().size() == 1) {
                    ctx.put(key, entry.getValue().get(0).getData());
                } else {
                    List<Map<String, Object>> maps = entry.getValue().stream().map(bd -> {
                        Map<String, Object> data = bd.getData();
                        data.keySet().forEach(s -> {
                            metadata.addFieldAsList(key+"."+s);
                        });
                        return data;
                    }).toList();
                    ctx.put(key, maps);
                }

            }
            File tmp = File.createTempFile(reportTemplate.getDocumentName(), "tmp");
            tmp.deleteOnExit();
            OutputStream out = new FileOutputStream(tmp);
            docReport.process(ctx, out);
            out.close();
            InputStream is = new FileInputStream(tmp);
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (XDocReportException e) {
            throw new RuntimeException(e);
        }
    }
}

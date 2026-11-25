package com.zzz.teachingevaluationsystembackend.parser.excel;

import com.zzz.teachingevaluationsystembackend.common.BizException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import java.util.HashMap;
import java.util.Map;

public class ExcelSheetReader {
    private final DataFormatter formatter = new DataFormatter();
    private final Map<String, Integer> headerIndexMap;

    public ExcelSheetReader(Row headerRow) {
        if (headerRow == null) throw new BizException(400, "Excel表头为空");
        this.headerIndexMap = new HashMap<>();
        for (int col = headerRow.getFirstCellNum(); col < headerRow.getLastCellNum(); col++) {
            if (col < 0) continue;
            String key = formatter.formatCellValue(headerRow.getCell(col)).trim();
            if (!key.isEmpty()) headerIndexMap.put(key, col);
        }
    }

    public int requiredCol(String... keys) {
        for (String key : keys) {
            Integer index = headerIndexMap.get(key);
            if (index != null) return index;
        }
        throw new BizException(400, "Excel缺少必填列：" + String.join(" / ", keys));
    }

    public int optionalCol(String... keys) {
        for (String key : keys) {
            Integer index = headerIndexMap.get(key);
            if (index != null) return index;
        }
        return -1;
    }

    public String cell(Row row, int col) {
        return formatter.formatCellValue(row.getCell(col)).trim();
    }
}

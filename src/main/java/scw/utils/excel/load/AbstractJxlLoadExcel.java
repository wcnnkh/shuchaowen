package scw.utils.excel.load;

import java.io.File;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import scw.common.Logger;
import scw.common.utils.ConfigUtils;

public abstract class AbstractJxlLoadExcel<T> extends AbstractLoadRow<T> implements Runnable, LoadRow {
	private final File excel;
	
	public AbstractJxlLoadExcel(String excel, Class<T> type, int nameMappingIndex, int beginRowIndex, int endRowIndex){
		this(ConfigUtils.getFile(excel), type, nameMappingIndex, beginRowIndex, endRowIndex);
	}
	
	public AbstractJxlLoadExcel(File excel, Class<T> type, int nameMappingIndex, int beginRowIndex, int endRowIndex){
		super(type, nameMappingIndex, beginRowIndex, endRowIndex);
		this.excel = excel;
	}

	public void run() {
		Logger.info(this.getClass().getName(), "开始读取" + excel.getName());
		long t = System.currentTimeMillis();
		Workbook workbook = null;
		try {
			workbook = Workbook.getWorkbook(excel);
			Sheet[] sheets = workbook.getSheets();
			for (int sheetIndex = 0; sheetIndex < sheets.length; sheetIndex++) {
				Sheet sheet = sheets[sheetIndex];
				for (int rowIndex = 0; rowIndex < sheet.getRows(); rowIndex++) {
					int columns = sheet.getColumns();
					String[] contents = new String[columns];
					for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
						Cell cell = sheet.getCell(columnIndex, rowIndex);
						String content = cell.getContents();
						if (content != null) {
							content = content.trim();
						}
						contents[columnIndex] = content;
					}

					load(sheetIndex, rowIndex, contents);
				}
			}
			Logger.info(this.getClass().getName(),
					"加载" + excel.getName() + "完成, 用时：" + (System.currentTimeMillis() - t) + "ms");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (workbook != null) {
				workbook.close();
			}
		}
	}

}
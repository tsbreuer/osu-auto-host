package lt.ekgame.autohost.plugins;
/*
 * WRITTEN BY EKGAME
 * https://github.com/ekgame/storasbot/blob/master/src/main/java/lt/ekgame/storasbot/utils/TableRenderer.java
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

public class TableRenderer {
	
	private int width;
	private List<Object> header;
	private List<List<Object>> table = new ArrayList<>();
	
	private String empty = "";
	
	public TableRenderer() {
		
	}
	
	public void setHeader(Object... header) {
		this.header = Arrays.asList(header);
		if (header.length > this.width)
			this.width = header.length;
	}

	public void addRow(Object... header) {
		List<Object> objects = Arrays.asList(header);
		table.add(objects);
		if (header.length > this.width)
			this.width = header.length;
	}
	
	public void setEmptyString(String str) {
		this.empty = str;
	}
	
	private String[][] normalizeTable() {
		int height = header == null ? table.size() : (table.size() + 1);
		String[][] normalized = new String[height][width];
		
		int vIndex = 0;
		if (header != null) {
			for (int hIndex = 0; hIndex < width; hIndex++) {
				if (header.size() > hIndex)
					normalized[vIndex][hIndex] = header.get(hIndex).toString();
				else
					normalized[vIndex][hIndex] = this.empty;
			}
			vIndex++;
		}
		
		for (List<Object> obj : table) {
			for (int hIndex = 0; hIndex < width; hIndex++) {
				if (obj.size() > hIndex)
					normalized[vIndex][hIndex] = obj.get(hIndex).toString();
				else
					normalized[vIndex][hIndex] = this.empty+"s";
			}
			vIndex++;
		}
		
		return normalized;
	}
	
	private int[] getCollumnWidths(String[][] table, int padding) {
		int collums[] = new int[width];
		for (int vIndex = 0; vIndex < table.length; vIndex++)
			for (int hIndex = 0; hIndex < width; hIndex++)
				if (table[vIndex][hIndex].length() + padding > collums[hIndex])
					collums[hIndex] = table[vIndex][hIndex].length() + padding;
		collums[collums.length-1] -= padding;
		return collums;
	}
	
	private String buildElement(String element, int width, String emptyChar) {
		String result = element;
		if (result.length() < width)
			result += StringUtils.repeat(emptyChar, width - result.length());
		return result;
	}
	
	private String buildLine(String[] strings, int[] widths, boolean header) {
		String line = IntStream.range(0, strings.length)
			.mapToObj((i) -> buildElement(strings[i], widths[i], " "))
			.collect(Collectors.joining("│"));
		
		if (header) {
			String seperator = IntStream.range(0, strings.length)
					.mapToObj((i) -> buildElement("", widths[i], "═"))
					.collect(Collectors.joining("╪"));
			line += "\n" + seperator;
		}
		return line;
	}
	
	public String build() {
		String[][] table = normalizeTable();
		int[] widths = getCollumnWidths(table, 1);
		return IntStream.range(0, table.length)
			.mapToObj(i -> buildLine(table[i], widths, header != null && i==0))
			.collect(Collectors.joining("\n"));
	}
}
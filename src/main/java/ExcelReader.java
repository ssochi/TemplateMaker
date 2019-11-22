import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.CellData;
import com.alibaba.excel.read.listener.ReadListener;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author wanqilin
 * @date 2019/11/19
 * @description
 */
public class ExcelReader {

    private static String outputPath = "tab/";
    private static String dataOutputPath = "tab/data/";
    private static final List<String> classNames = new LinkedList<>();
    private static final String TAB = "    ";

    public static void main(String[] args) throws IOException {
        if (args.length < 3){
            System.out.println("java -jar template-maker.jar outputPath dataOutputPath excelPath");
            return;
        }

        outputPath = args[0];
        dataOutputPath = args[1];
        String excelPath = args[2];

        init();

        Files.walk(Paths.get(excelPath)).sorted(Comparator.reverseOrder()).map(Path::toFile)
                .peek(System.out::println).forEach(file -> {
                    if (file.getPath().endsWith(".xlsx") || file.getPath().endsWith(".xlsm")){
                        try {
                            readExcel(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
        });

    }

    private static void init() throws IOException {
        if (new File(outputPath).exists()){
            Files.walk(Paths.get(outputPath)).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .peek(System.out::println).forEach(File::delete);
        }
        if (new File(dataOutputPath).exists()){
            Files.walk(Paths.get(dataOutputPath)).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .peek(System.out::println).forEach(File::delete);
        }

        mkdirIfNotExist(outputPath);
        mkdirIfNotExist(dataOutputPath);
    }

    private static void mkdirIfNotExist(String path) {
        String[] splits = path.split("/");
        StringBuilder ps = new StringBuilder();
        for (String p : splits) {
            ps.append(p).append("/");
            File f = new File(ps.toString());
            if (!f.exists() && !f.mkdir()){
                throw new IllegalStateException(path);
            }
        }
    }


    private static void readExcel(File excel) throws IOException {
        int maxSheetId = getMaxSheetId(excel);

        for (int sheetId = 0; sheetId <= maxSheetId ; sheetId++) {
            Map<Integer,CellData> headMap = excelHeadMap(excel,sheetId);
            List<String> params = new LinkedList<>();
            List<String> paramTypes = new LinkedList<>();
            String[] className = new String[1];
            headMap.forEach((k,v) -> {
                int id = k;
                String[] info = getParamInfos(v.getStringValue());
                if (info.length < 2)
                    throw new IllegalStateException("wrong input paramStr : " + v.getStringValue());
                if (id == 0){
                    params.add(info[0]);
                    paramTypes.add("int");
                    className[0] = "T" + toUpCaseFirstWord(info[1]);
                }else{
                    params.add(info[0]);
                    paramTypes.add(info[1]);
                }
            });
            classNames.add(className[0]);
            CSharpCodeGenerator generator = new CSharpCodeGenerator(className[0],params,paramTypes);

            RandomAccessFile file = new RandomAccessFile(new File(outputPath + className[0] + ".cs"),"rw");
            file.write(generator.gen().getBytes());
            file.close();

            file = new RandomAccessFile(new File(dataOutputPath + className[0]),"rw");

            RandomAccessFile finalFile = file;
            EasyExcel.read(excel, new ReadListener() {
                @Override
                public void onException(Exception exception, AnalysisContext context) throws Exception {

                }

                @Override
                public void invokeHead(Map headMap, AnalysisContext context) {

                }

                @SuppressWarnings("unchecked")
                @Override
                public void invoke(Object data, AnalysisContext context) {
                    LinkedHashMap<Integer,String> dataMap = (LinkedHashMap<Integer, String>) data;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < dataMap.size(); i++) {
                        sb.append(dataMap.get(i));
                        if (i != dataMap.size() - 1){
                            sb.append("|");
                        }
                    }
                    sb.append('\n');
                    try {
                        finalFile.write(sb.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    try {
                        finalFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public boolean hasNext(AnalysisContext context) {
                    return true;
                }
            }).sheet(sheetId).doReadSync();

        }

        genAllTable(classNames);
    }

    private static void genAllTable(List<String> classNames) throws IOException {
        StringBuilder sb = new StringBuilder();
        String tab = TAB;
        sb.append("public class AllTemplate{\n");
        sb.append(tab).append("public static void LoadAll(string path){\n");
        tab += TAB;

        for (String className : classNames) {
            sb.append(tab).append(className).append(".Load(path);\n");
        }
        tab = tab.substring(0,tab.length() - 4);
        sb.append(tab).append("}\n");
        tab = tab.substring(0,tab.length() - 4);
        sb.append(tab).append("}\n");

        RandomAccessFile file = new RandomAccessFile(outputPath + "AllTemplate.cs","rw");
        file.write(sb.toString().getBytes());
        file.close();
    }

    static String[] getParamInfos(String paramStr){
        int idx1 = paramStr.indexOf("(");
        int idx2 = paramStr.lastIndexOf(")");
        if (idx1 == idx2 || idx1 < 0 || idx2 < 0)
            throw new IllegalStateException("wrong input paramStr : " + paramStr);

        String tmp = paramStr.substring(idx1 + 1,idx2);
        return tmp.split(",");
    }

    private static String toUpCaseFirstWord(String words){
        if (words.length() == 1){
            return words.toUpperCase();
        }
        return words.substring(0,1).toUpperCase() + words.substring(1);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    static int getMaxSheetId(File excel){
        int max = 0;
        while (excelHeadMap(excel,max++).size() != 0);
        return max - 2;
    }

    static Map<Integer, CellData> excelHeadMap(File excel, int sheetId){
        Map<Integer,CellData> headMap = new HashMap<>();
        EasyExcel.read(excel, new ReadListener() {
            @Override
            public void onException(Exception exception, AnalysisContext context) throws Exception {

            }

            @SuppressWarnings("unchecked")
            @Override
            public void invokeHead(Map map, AnalysisContext context) {
                headMap.putAll(map);
            }

            @Override
            public void invoke(Object data, AnalysisContext context) {

            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {

            }

            @Override
            public boolean hasNext(AnalysisContext context) {
                return false;
            }
        }).sheet(sheetId).doReadSync();
        return headMap;
    }
}

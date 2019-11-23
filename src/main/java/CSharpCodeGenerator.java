import java.util.LinkedList;
import java.util.List;

/**
 * @author wanqilin
 * @date 2019/11/19
 * @description
 */
public class CSharpCodeGenerator {
    private String className;
    private List<String> paramNames;
    private List<String> paramTypes;
    private List<String> params;
    public static final String space = " ";
    public static final String TAB = "    ";

    public CSharpCodeGenerator(String className, List<String> paramNames, List<String> paramTypes) {
        this.className = className;
        this.paramNames = paramNames;
        this.paramTypes = paramTypes;
        params = new LinkedList<>();
        for (String paramName : paramNames) {
            params.add("_" + paramName);
        }
    }

    public String gen(){
        StringBuilder sb = new StringBuilder();
        String tab = "";
        sb.append("//generate by auto generator , do not edit !!!\n");
        sb.append("using System.Collections.Generic;\n" +
                "using System.IO;\n\n");
        sb.append(tab).append("public class ").append(className).append("{\n");
        tab += TAB;
        for (int i = 0; i < paramNames.size(); i++) {
            String name = params.get(i);
            String type = paramTypes.get(i);
            sb.append(tab)
                    .append("private ").append(type).append(space).append(name).append(";\n");
        }
        sb.append(tab)
                .append(String.format("private static readonly Dictionary<int,%s> Dict = new Dictionary<int, %s>();\n",className,className))
                .append(tab).append("public static void Load(string path){\n");
        tab += TAB;

        sb.append(tab).append("var line = \"\";\n");
        sb.append(tab).append("var sr = new StreamReader(path);\n");
        sb.append(tab).append("while ((line = sr.ReadLine()) != null){\n");
        tab += TAB;
        sb.append(tab).append("var data = line.Split('|');\n");
        sb.append(tab).append("var t").append(" = new ").append(className).append("();\n");
        for (int i = 0; i < paramNames.size(); i++) {
            String name = params.get(i);
            String type = paramTypes.get(i);

            if (!isList(type)){
                sb.append(tab).append("t.").append(name).append(" = ");
                if (type.equals("string")){
                    sb.append(String.format("data[%d];\n",i));
                }else{
                    sb.append(String.format("%s.Parse(data[%d]);\n",type,i));
                }
            }else{
                String subType = getSubType(paramTypes.get(i));
                sb.append(tab).append(String.format("var arr%d = data[%d].Split(',');\n",i,i));
                sb.append(tab).append(String.format("t.%s = new List<%s>();\n",params.get(i),subType));
                sb.append(tab).append(String.format("for(var k = 0;k < arr%d.Length;k++){\n",i));
                tab += TAB;
                String process = subType.equals("string") ?
                        String.format("arr%d[k]",i) : String.format("%s.Parse(arr%d[k])",subType,i);

                sb.append(tab).append(String.format("t.%s.Add(%s);\n",params.get(i),process));
                tab = tab.substring(0,tab.length() - 4);
                sb.append(tab).append("}\n");
            }
        }
        sb.append(tab).append(String.format("Dict.Add(t.%s,t);\n",params.get(0)));

        tab = tab.substring(0,tab.length() - 4);
        sb.append(tab).append("}\n");
        tab = tab.substring(0,tab.length() - 4);
        sb.append(tab).append("}\n");

        for (int i = 0; i < paramNames.size(); i++) {
            String name = params.get(i);
            String type = paramTypes.get(i);
            type = isList(type) ? "IReadOnlyList<" + getSubType(type) + ">" : type;
            sb.append(tab).append(String.format("public %s Get%s(){\n",type,toUpCaseFirstWord(paramNames.get(i))));
            tab += TAB;
            sb.append(tab).append(String.format("return %s;\n",name));
            tab = tab.substring(0,tab.length() - 4);
            sb.append(tab).append("}\n");
        }
        sb.append(tab).append(String.format("public static %s GetById(int id){\n",className));
        tab += TAB;
        sb.append(tab).append("return Dict.ContainsKey(id) ? Dict[id] : null;\n");
        tab = tab.substring(0,tab.length() - 4);
        sb.append(tab).append("}\n");

        sb.append(tab).append(String.format("public static Dictionary<int,%s> GetDataMap(){\n",className));
        tab += TAB;
        sb.append(tab).append("return Dict;\n");
        tab = tab.substring(0,tab.length() - 4);
        sb.append(tab).append("}\n");

        tab = tab.substring(0,tab.length() - 4);
        sb.append(tab).append("}\n");
        return sb.toString();
    }

    private String getSubType(String paramStr) {
        int idx1 = paramStr.indexOf("<");
        int idx2 = paramStr.lastIndexOf(">");
        if (idx1 == idx2 || idx1 < 0 || idx2 < 0)
            throw new IllegalStateException("wrong input getSubType : " + paramStr);

        return paramStr.substring(idx1 + 1,idx2);
    }

    private boolean isList(String type) {
        return type.contains("<");
    }

    private String toUpCaseFirstWord(String words){
        if (words.length() == 1){
            return words.toUpperCase();
        }
        return words.substring(0,1).toUpperCase() + words.substring(1);
    }

}

##TemplateMaker

为excel生成C#读取类

###格式
一个sheet的第一行存储了整张表的数据结构信息

第一列为Id,因此必为int类型，只需定义它的名字
> (列名)解释

之后的列 可以定义它的列名和类型
> (列名,类型)解释

列名必须为全英文，建议使用驼峰法命名，首字母小写
类型包含：int,long,float,string,List<int>,List<long>,List<float>,List<string>
注：string中不能有'|',当为List<string>时不能含','

###运行

> java -jar template-maker.jar outputPath dataOutputPath excelPath

其中 outputPath dataOutputPath excelPath 都为相对路径









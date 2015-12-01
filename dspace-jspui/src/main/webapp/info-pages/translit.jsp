<%@ page import="java.util.Scanner" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.util.ArrayList" %>
<%--
  Created by IntelliJ IDEA.
  User: flash
  Date: 26.05.11
  Time: 19:51
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>Simple jsp page</title></head>
<body>
<form name="translit" method="POST" action="./translit.jsp">
    <label>Enter your name to translit</label>
    <input type="text" class="textfield" name="name" width="300"/>
    <input type="submit" class="submit" value="Translit" />
</form>
<% if (request.getParameter("name")!=null){

    String alphabet = new String(""+
                    "Зг зг Zgh zgh "+
                    "А а 	A a "+
                    "Б б 	B b "+
                    "В в 	V v "+
                    "Г г 	H h "+
                    "Ґ ґ 	G g "+
                    "Д д 	D d "+
                    "Е е 	E e "+
                    "Є є 	Ye ie "+
                    "Ж ж 	Zh zh "+
                    "З з 	Z z "+
                    "И и 	Y y "+
                    "І і 	I i "+
                    "Ї ї 	Yi i "+
                    "Й й 	Y i "+
                    "К к 	K k "+
                    "Л л 	L l "+
                    "М м 	M m "+
                    "Н н 	N n "+
                    "О о 	O o "+
                    "П п 	P p "+
                    "Р р 	R r "+
                    "С с 	S s "+
                    "Т т 	T t "+
                    "У у 	U u "+
                    "Ф ф 	F f "+
                    "Х х 	Kh kh "+
                    "Ц ц 	Ts ts "+
                    "Ч ч 	Ch ch "+
                    "Ш ш 	Sh sh "+
                    "Щ щ 	Shch shch "+
                    "Ю ю 	Yu iu "+
                    "Я я 	Ya ia ");
    class ReplaceVariant{
        String variant, replacement;
        public ReplaceVariant(String v, String r){
            this.variant = v;
            this.replacement = r;
        }
    }
    String s = new String(request.getParameter("name").getBytes("ISO-8859-1"),"UTF8");
    s = s.replace("'","").replace("ь","");
    ArrayList<ReplaceVariant> alpha = new ArrayList<ReplaceVariant>();
    Scanner in = new Scanner(alphabet);
    String[] variant = new String[2];
    String[] replacement = new String[2];
    while(in.hasNext()){
        variant[0] = in.next();
        variant[1] = in.next();
        replacement[0] = in.next();
        replacement[1] = in.next();
        alpha.add(new ReplaceVariant(variant[0], replacement[0]));
        alpha.add(new ReplaceVariant(variant[1], replacement[1]));
    }
    for(int i=0;i<alpha.size();++i){
        s = s.replace(alpha.get(i).variant, alpha.get(i).replacement);
    }
    PrintWriter res = response.getWriter();
    response.setCharacterEncoding("UTF8");
    out.flush();
    res.println("Translit variant of your name is:<span class=\"trname\">"+s+"</span><br /><br />");
}
    %>
</body>
</html>
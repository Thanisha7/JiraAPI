import io.restassured.RestAssured;
import io.restassured.filter.session.SessionFilter;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

import static io.restassured.RestAssured.given;

public class Tests {

    JsonPath js;
    SessionFilter session = new SessionFilter();
    int issueId,commentId;
    String comment="Comment!!!!!123";

    @Test
    public void login()
    {
        Response response=given()
                .header("Content-Type","application/json")
                .body("{ \"username\": \"Thanisha\", \"password\": \"Thsm14is@\" }").filter(session)
                .post("http://localhost:8080/rest/auth/1/session");

        Assert.assertEquals(response.statusCode(),200);
    }

    @Test(dependsOnMethods = "login")
    public void createIssue()
    {

       Response response= given()
               .header("Content-Type","application/json")
               .header("Accept-Encoding","gzip, deflate, br")
                .body("{\n" +
                        "    \"fields\": {\n" +
                        "        \"project\": {\n" +
                        "            \"key\": \"RES\"\n" +
                        "        },\n" +
                        "        \"summary\": \"Debit card issue\",\n" +
                        "        \"description\": \"Debit card issue\",\n" +
                        "        \"issuetype\": {\n" +
                        "            \"name\": \"Bug\"\n" +
                        "        }\n" +
                        "        \n" +
                        "    }  \n" +
                        "}")
               .filter(session).post("http://localhost:8080/rest/api/2/issue");
       Assert.assertEquals(response.statusCode(),201);
       String res=response.asString();
       js=new JsonPath(res);
       issueId=js.getInt("id");
        System.out.println("Issue ID: "+issueId);
    }
    @Test(dependsOnMethods = "createIssue")
    public void addComment()
    {
        Response  response=given().header("Content-Type","application/json")
            .header("Accept-Encoding","gzip, deflate, br").pathParam("id",issueId)
                .body("{\n" +
                        "    \"body\": \""+comment+"\",\n" +
                        "    \"visibility\": {\n" +
                        "        \"type\": \"role\",\n" +
                        "        \"value\": \"Administrators\"\n" +
                        "    }\n" +
                        "}")
                .filter(session).post("http://localhost:8080/rest/api/2/issue/{id}/comment");
        Assert.assertEquals(response.statusCode(),201);
        js=new JsonPath(response.asString());
        commentId=js.getInt("id");
    }
    @Test(dependsOnMethods = "createIssue")
    public void addAttachment()
    {
        Response response=given().header("X-Atlassian-Token","no-check").filter(session).pathParam("id",issueId)
                .multiPart("file",new File("jira.txt")).header("Content-Type","multipart/form-data")
                .post("http://localhost:8080/rest/api/2/issue/{id}/attachments");
        Assert.assertEquals(response.statusCode(),200);
    }

    @Test(dependsOnMethods = "createIssue")
    public void getIssueAndComment()
    {
       Response response= given().pathParam("id",issueId).filter(session).queryParam("fields","comment")
                .get("http://localhost:8080/rest/api/2/issue/{id}");
       Assert.assertEquals(response.statusCode(),200);
        System.out.println(response.asString());
        js=new JsonPath(response.asString());
        int size=js.getInt("fields.comment.comments.size()");
        for (int i=0;i<size;i++)
        {
            int cid=js.getInt("fields.comment.comments["+i+"].id");
            if (cid==commentId)
            {
                System.out.println(js.getString("fields.comment.comments["+i+"].body"));
            }
        }

    }
}

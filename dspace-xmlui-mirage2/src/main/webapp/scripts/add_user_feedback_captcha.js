function add_captcha (){
    li=document.getElementById("aspect_artifactbrowser_FeedbackForm_item_captcha");
    captcha=li.getElementsByTagName("div")[0];
    //Create captcha text
    captcha_input=document.getElementById("aspect_artifactbrowser_FeedbackForm_field_captcha_input");

    var number1=Math.floor((Math.random() * 20));
    var number2=Math.floor((Math.random() * 10));
    captcha_text=document.createElement("p");
    captcha_text.innerHTML="<b>"+ number1.toString()+" + "+number2.toString()+" = "+"</b>";
    captcha_text.setAttribute("class","captcha");
    captcha.insertBefore(captcha_text,captcha_input);

    var hidden_number1 = createHiddenNumber("number1",number1);
    var hidden_number2 = createHiddenNumber("number2",number2);

    captcha.appendChild(hidden_number1);
    captcha.appendChild(hidden_number2);

    setUserFeedbackInputsRequired();
}

function createHiddenNumber(name,value){
    hidden_number = document.createElement("input");
    hidden_number.setAttribute("type", "hidden");
    hidden_number.setAttribute("name", name);
    hidden_number.setAttribute("value", value);
    return hidden_number;
}

function setUserFeedbackInputsRequired(){
    document.getElementById("aspect_artifactbrowser_FeedbackForm_field_email").setAttribute("required","true");
    document.getElementById("aspect_artifactbrowser_FeedbackForm_field_comments").setAttribute("required","true");
    document.getElementById("aspect_artifactbrowser_FeedbackForm_field_captcha_input").setAttribute("required","true");
}
window.onload =	initPage;

function initPage()	{
	//alert("Inside initPage()");
	postButton = document.getElementById("addPost");
	voteArticleButton = document.getElementById("addVoteForArticle");
	
	postButton.onclick = function() {
		//alert("Inside postButton function()");
		userText = document.getElementById("userTextInput").value;
		articleText = document.getElementById("articleTextInput").value;
		if (userText == "") {
			alert("Please enter a User Name");
		} else {
			//alert("userText="+userText);
		}
		if (articleText == "") {
			alert("Please enter a Article Name");
		} else {
			//alert("articleText="+articleText);
		}		
		relativeURL = 'user='+userText+'&post='+articleText;			
		getDetails(relativeURL);
	}
	
	voteArticleButton.onclick = function() {
		//alert("Inside voteArticleButton function()");
		userText = document.getElementById("userTextInput").value;
		voteArticleText = document.getElementById("voteArticleTextInput").value;		
		relativeURL = 'user='+userText+'&vote='+voteArticleText;			
		getDetails(relativeURL);
	}
}

function getDetails(relativeURL) 
{
	//alert("Inside getDetails()");
	request	= createRequest();
	if (request	== null) {
		alert("Unable to create	request");
		return;
	}
	var url= "http://localhost:1234?"+escape(relativeURL);
	//alert("url="+url);
	request.open("GET",	url, true);
	request.onreadystatechange = displayDetails;
	request.send(null);

}


function createRequest() {
  try {
	request	= new XMLHttpRequest();
  }	catch (tryMS) {
	try	{
	  request =	new	ActiveXObject("Msxml2.XMLHTTP");
	} catch	(otherMS) {
	  try {
		request	= new ActiveXObject("Microsoft.XMLHTTP");
	  }	catch (failed) {
		request	= null;
	  }
	}
  }
  return request;
}




function displayDetails() {
	//alert("Inside displayDetails() with request.readyState = "+request.readyState);
	if (request.readyState == 4) {
		//alert("request.readyState == 4. Means Server has finished sending data to the client");		
		if (request.status == 200) {
			alert("request.status == 200. Means data transfer successful");
			detailDiv =	document.getElementById("detailsPane");
			//alert("The Data transferred is -> request.responseText = "+request.responseText);
			detailDiv.innerHTML = request.responseText;
		}
	}
}

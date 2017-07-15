(function () {
    'use strict';

    angular
        .module('app')
        .factory('CoinService', CoinService);

   CoinService.$inject = ['$http'];
    function CoinService($http) {
        var service = {};
        
        var urlDateCache ={};
        var urlResultCache={};
        var cacheTimeOut=10;
        var requestsCached = 0;
        

        service.getAllCoins = getAllCoins;
        service.getAllCoinStats = getAllCoinStats;
        service.getTop10Stats = getTop10Stats;
        

        return service;

        
        function getAllCoins(page, size, params, callback) {
        	if(requestsCached === 100){
				requestsCached = 0;
				urlDateCache ={};
			    urlResultCache={};
			}
        	var url = "/api/coins";
        	var start = false;
        	if(size != null && page != null){
        		url = url+"?page="+page+"&size="+size;
        		start = true;
        	}
        	var index, len;
        	for (index = 0, len = params.length; index < len; ++index) {
        		if(params[index]["name"] && params[index]["value"]){
        			if(start){
        				url=url+"&"+params[index]["name"]+"="+params[index]["value"];
        			}
        			else{
        				url=url+"?"+params[index]["name"]+"="+params[index]["value"];
        				start=true;
        			}
        		}
        	}
        	
        	var currentDate = new Date();
        	if(urlDateCache[url] === undefined || Math.round((currentDate.getTime() - urlDateCache[url].getTime())/60000) >= cacheTimeOut){
        		
        		$http.get(url).then(function successCallback(response) {
        			urlDateCache[url]=currentDate;
        			urlResultCache[url]={ success: true, message: response.data};
        			requestsCached++;
					callback(urlResultCache[url]);
				}, function errorCallback(response) {
					  if(response.data.message != null){
							callback({ success: false, message: response.data.message});
					  } else if(response.data.error != null){
	 					  var i, len = response.data.error.length;
	 					  var message = "";
	 					  for(i = 0; i<len;i++){
	 						 message += response.data.error[i]+"\n";
	 					  }
							callback({ success: false, message: message});
					  }
					  else{
							callback({ success: false, message:'Error loading coins'});
					  }
				});
        	} else {
        		
        		callback(urlResultCache[url]);
        	}

		}
        
        function getAllCoinStats(page, size, params, callback) {
        	if(requestsCached === 100){
				requestsCached = 0;
				urlDateCache ={};
			    urlResultCache={};
			}
        	var url = "/api/bizstats";
        	var start = false;
        	if(size != null && page != null){
        		url = url+"?page="+page+"&size="+size;
        		start = true;
        	}
        	var index, len;
        	for (index = 0, len = params.length; index < len; ++index) {
        		if(params[index]["name"] && params[index]["value"]){
        			if(start){
        				url=url+"&"+params[index]["name"]+"="+params[index]["value"];
        			}
        			else{
        				url=url+"?"+params[index]["name"]+"="+params[index]["value"];
        				start=true;
        			}
        		}
        	}
        	
        	var currentDate = new Date();
        	if(urlDateCache[url] === undefined || Math.round((currentDate.getTime() - urlDateCache[url].getTime())/60000) >= cacheTimeOut){
        		
        		$http.get(url).then(function successCallback(response) {
        			urlDateCache[url]=currentDate;
        			urlResultCache[url]={ success: true, message: response.data};
					callback(urlResultCache[url]);
				}, function errorCallback(response) {
					  if(response.data.message != null){
							callback({ success: false, message: response.data.message});
					  } else if(response.data.error != null){
	 					  var i, len = response.data.error.length;
	 					  var message = "";
	 					  for(i = 0; i<len;i++){
	 						 message += response.data.error[i]+"\n";
	 					  }
							callback({ success: false, message: message});
					  }
					  else{
							callback({ success: false, message:'Error loading bizstats'});
					  }
				});
        	} else {
        		
        		callback(urlResultCache[url]);
        	}
		}
        
        function getTop10Stats(callback) {
        	if(requestsCached === 100){
				requestsCached = 0;
				urlDateCache ={};
			    urlResultCache={};
			}
        	var url = "/api/bizstats/top10";
        	var currentDate = new Date();
        	if(urlDateCache[url] === undefined || Math.round((currentDate.getTime() - urlDateCache[url].getTime())/60000) >= cacheTimeOut){
        		
        		$http.get(url).then(function successCallback(response) {
        			urlDateCache[url]=currentDate;
        			urlResultCache[url]={ success: true, message: response.data};
					callback(urlResultCache[url]);
				}, function errorCallback(response) {
					  if(response.data.message != null){
							callback({ success: false, message: response.data.message});
					  } else if(response.data.error != null){
	 					  var i, len = response.data.error.length;
	 					  var message = "";
	 					  for(i = 0; i<len;i++){
	 						 message += response.data.error[i]+"\n";
	 					  }
							callback({ success: false, message: message});
					  }
					  else{
							callback({ success: false, message:'Error loading bizstats'});
					  }
				});
        	} else {
        		
        		callback(urlResultCache[url]);
        	}
		}

        
    }

})();

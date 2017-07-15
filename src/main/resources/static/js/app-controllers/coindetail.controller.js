(function () {
    'use strict';

    angular
        .module('app')
        .controller('DetailViewController', DetailViewController);

    DetailViewController.$inject = ['$state', '$scope', '$http', 'CoinService', '$filter', '$stateParams'];
    function DetailViewController($state, $scope, $http, CoinService, $filter, $stateParams) {
        var vm = this;
		vm.loadCoinInfo = loadCoinInfo;
		vm.chartClick = chartClick;
		vm.reloadCoinInfo = reloadCoinInfo
		vm.open1 = open1;
		vm.open2 = open2;
		
		vm.coinstats = [];
		vm.error = {
				hidden: true,
				message: ""
		};
		vm.currentcoin = {};
		vm.currentstats = {};
		vm.from = {};
		vm.to = {};
		
		vm.labels = [];
		vm.series = ['Hype', 'Price USD'];
		vm.data = [[],[]];
		vm.datasetOverride = [{ yAxisID: 'y-axis-1' }, { yAxisID: 'y-axis-2' }];
		vm.chartcolors = [ '#EA3925', '#1CA539', '#DCDCDC', '#46BFBD', '#FDB45C', '#949FB1', '#4D5360'];
		vm.options = {
		    scales: {
		      yAxes: [
		        {
		          id: 'y-axis-1',
		          type: 'linear',
		          display: true,
		          position: 'left'
		        },
		        {
		          id: 'y-axis-2',
		          type: 'linear',
		          display: true,
		          position: 'right'
		        }
		      ]
		    }
		  };
		
		vm.popup1 = { opened: false};
		vm.popup2 = { opened: false};
		
		
		$('.dropdown-menu').find('input').click(function (e) {
		    e.stopPropagation();
		});
		
		init();
		
		function init(){
			loadCoinInfo($stateParams.coin)
		}
		
		function loadCoinInfo(coin) {
			vm.currentcoin = coin;
			vm.currentstats = null;
			
			vm.to = new Date();
			vm.to.setDate(vm.to.getDate()+1);
			var to = $filter('date')(vm.to, "yyyy-MM-dd");
			vm.from = new Date();
			vm.from.setDate(vm.from.getDate()-7);
			var from = $filter('date')(vm.from, "yyyy-MM-dd");
			
			CoinService.getAllCoinStats(null, null,[{name: "sort", value: 'id,asc'}, {name: "coinId", value: vm.currentcoin.id}, {name: "from", value: from}, {name: "to", value: to}], function (result) {
            	if(result.success == true){
            		vm.coinstats = result.message.content;
            		vm.currentstats = vm.coinstats[vm.coinstats.length-1];
            		vm.error.hidden = true;
            		drawChart();
            		
            	} else {
            		showAlert(result.message);
            	}
                
            });
	

		}
		
		function reloadCoinInfo(){
			var to = $filter('date')(vm.to, "yyyy-MM-dd");
			var from = $filter('date')(vm.from, "yyyy-MM-dd");
			
			CoinService.getAllCoinStats(null, null,[{name: "sort", value: 'id,asc'}, {name: "coinId", value: vm.currentcoin.id}, {name: "from", value: from}, {name: "to", value: to}], function (result) {
            	if(result.success == true){
            		vm.coinstats = result.message.content;
            		vm.currentstats = vm.coinstats[vm.coinstats.length-1];
            		vm.error.hidden = true;
            		drawChart();
            		
            	} else {
            		showAlert(result.message);
            	}
                
            });
			
		}
		
		function open1() {
		   vm.popup1.opened = true;
		  };

		function open2() {
		    vm.popup2.opened = true;
		};
		
		function drawChart(){
			var labels = [];
			var hype = [];
			var priceusd = [];
			var date;
			for (var i = 0; i < vm.coinstats.length; i++) {
			    date = new Date(vm.coinstats[i].date);
			    labels.push($filter('date')(date, "dd.MM.yyyy HH:mm"));
			    hype.push(vm.coinstats[i].nrThreads+vm.coinstats[i].nrPosts);
			    priceusd.push(vm.coinstats[i].price_usd);
			    
			}
			
			vm.labels = labels;
			vm.data = [hype, priceusd];
			if(labels.length === 0){
				showAlert("No data available for that time frame!")
			}
		}
		
		function chartClick(points, evt){
			vm.currentstats = vm.coinstats[points[0]._index];
			console.log(vm.currentstats.date);
			$scope.$apply();
		}
		
		function showAlert(message){
			vm.error.message = message;
			vm.error.hidden = false;
		}

        

        
       
    }

})();
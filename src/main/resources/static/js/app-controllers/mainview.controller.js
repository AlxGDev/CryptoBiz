(function () {
    'use strict';

    angular
        .module('app')
        .controller('MainViewController', MainViewController);

    MainViewController.$inject = ['$state', '$scope', '$http', 'CoinService', '$filter'];
    function MainViewController($state, $scope, $http, CoinService, $filter) {
        var vm = this;

		vm.coinstats = [];
		vm.error = {
				hidden: true,
				message: ""
		};
		
		
		vm.popup1 = { opened: false};
		vm.popup2 = { opened: false};
		
		
		$('.dropdown-menu').find('input').click(function (e) {
		    e.stopPropagation();
		});
		
		init();
		
		function init(){
			loadTop10();
		}

		function loadTop10() {
			vm.coinstats= null;

			CoinService.getTop10Stats(function (result) {
            	if(result.success == true){
            		vm.coinstats = result.message;
            		vm.error.hidden = true;

            		
            	} else {
            		showAlert(result.message);
            	}
                
            });
	

		}

		function showAlert(message){
			vm.error.message = message;
			vm.error.hidden = false;
		}
		

        

        
       
    }

})();
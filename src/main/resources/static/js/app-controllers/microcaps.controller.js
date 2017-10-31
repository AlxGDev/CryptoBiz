(function () {
    'use strict';

    angular
        .module('app')
        .controller('MicroCapsController', MicroCapsController);

    MicroCapsController.$inject = ['$state', '$scope', '$http', 'CoinService', '$filter', '$stateParams'];
    function MicroCapsController($state, $scope, $http, CoinService, $filter, $stateParams) {
        var vm = this;
		vm.microcaps = [];
		vm.error = {
				hidden: true,
				message: ""
		};
		
		
		$('.dropdown-menu').find('input').click(function (e) {
		    e.stopPropagation();
		});
		
		init();
		
		function init(){
			loadMicroCaps();
		}
		
		function loadMicroCaps(){
			CoinService.getMicroCaps(function (result) {
            	if(result.success == true){
            		vm.microcaps = result.message;
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
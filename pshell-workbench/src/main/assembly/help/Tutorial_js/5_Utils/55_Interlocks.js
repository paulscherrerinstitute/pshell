/////////////////////////////////////////////////////////////////////////////////////////////////// 
// Interlocks: example on creating and installing device interlock rules.
/////////////////////////////////////////////////////////////////////////////////////////////////// 

//Motor and Positioners
var MyInterlock1 = Java.extend(Interlock)
var interlock1 = new MyInterlock1( [m1, p1]) {	    	    
    check: function (pos) {
    	m=pos[0]
    	p=pos[1]
        if ((p<500) && (m<5) && (m>4)){
            return false
        }
        return true        	
    },
}


/*
//Motor group
var MyInterlock2 = Java.extend(Interlock)
var interlock2 = new MyInterlock2( [mg1, p1]) {	    	    
    check: function (pos) {
    	print(pos)
    	mg=pos[0]
    	p=pos[1]
    	print(to_array(mg))
        if ((p<500) && (mg[0]<5) && (mg[1]>4)){
            return false
        }
        return true        	
    },
}
*/

/*
//Discrete Positioner
var MyInterlock3 = Java.extend(Interlock)
interlock3 = new MyInterlock3( [dp1, p1]) {	    	    
    check: function (pos) {
    	dp=pos[0]
    	p=pos[1]
    	print ("DP " + dp)
    	print ("P " + p)
        if ((p<500) && (dp=="Out")){
            return false
        }
        return true        	
    },
}
*/

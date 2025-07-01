################################################################################################### 
# Interlocks: example on creating and installing device interlock rules.
################################################################################################### 


class MyInterlock1 (Interlock):
#Motor and Positioners
    def __init__(self):
        Interlock.__init__(self, (m1, p1))

    def check(self, (m, p)):
        if p<500 and (m<5 and m>4):
            return False
        return True

interlock1 = MyInterlock1()

"""
#Motor group
class MyInterlock2(Interlock):
    def __init__(self):
        Interlock.__init__(self, (mg1, p1))

    def check(self, ((m1,m2), p)):
        if p<500 and (m1>4 and m2>4):
            return False
        return True

interlock2 = MyInterlock2()
"""


"""
#Discrete Positioner
class MyInterlock3(Interlock):
    def __init__(self):
        Interlock.__init__(self,  (dp1, p1))

    def check(self, (dp, p)):
        if p<500 and dp=="Out":
            return False
        return True

#interlock3 = MyInterlock3()
"""
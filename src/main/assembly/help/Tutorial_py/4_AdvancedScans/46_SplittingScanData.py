################################################################################################### 
# Save multi-pass continuous scans in separated datasets
################################################################################################### 

STEPS_M1 = 10
STEPS_M2 = 5
        

def before_pass(pass_num, scan):
    m2.move(0.0 + 0.2 * (pass_num-1))

r1 = cscan(m1, (ai1), 0.0, 1.0 , steps=STEPS_M1, passes = STEPS_M2+1, before_pass = before_pass, split=True, tag = "Step {index}" ) 
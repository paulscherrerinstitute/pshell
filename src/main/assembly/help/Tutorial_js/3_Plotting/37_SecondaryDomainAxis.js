///////////////////////////////////////////////////////////////////////////////////////////////////
// Creating a secondary X axis, using the underlying JFreeChart libraey API
/////////////////////////////////////////////////////////////////////////////////////////////////// 

importClass(org.jfree.chart.axis.NumberAxis)
importClass(org.jfree.chart.axis.LogarithmicAxis)

p = plot([1,2,3,4,5])[0]

x2 = new NumberAxis("Secondary X Axis")        //Linear scale
//x2 = new LogarithmicAxis("Secondary X Axis")  //Logarithmic scale
x2.setRange(0.1, 100)
p.getAxis(AxisId.X).setRange(-2, 5) // So the relation between X1 and X2 is enforced


jf = p.chart.plot
jf.setDomainAxis(1, x2 )

//This is just for formatting te new axis fonts and colors, equals to the first axis
jf.getDomainAxis(1).labelPaint = jf.getDomainAxis(0).labelPaint
jf.getDomainAxis(1).labelFont = jf.getDomainAxis(0).labelFont
jf.getDomainAxis(1).tickLabelPaint = jf.getDomainAxis(0).tickLabelPaint
jf.getDomainAxis(1).tickLabelFont = jf.getDomainAxis(0).tickLabelFont





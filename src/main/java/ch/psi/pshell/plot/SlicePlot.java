package ch.psi.pshell.plot;

/**
 *
 */
public interface SlicePlot extends Plot<SlicePlotSeries> {
    public void setPage(int page);
    
    default int getNumberOfPages(){
        if (getSeries(0)==null){
            return 0;
        }
        return getSeries(0).getNumberOfBinsZ();
    }
    
    default int getPage(){
        if (getSeries(0)==null){
            return -1;
        }
        return getSeries(0).getPage();
    }    

}

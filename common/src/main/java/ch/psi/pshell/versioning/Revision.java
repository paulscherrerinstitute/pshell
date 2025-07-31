/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ch.psi.pshell.versioning;

import ch.psi.pshell.utils.Chrono;

/**
 * Entity class holding the information of a certain commit of the repository.
 */
public class Revision {

    public String id;
    public long timestamp;
    public String commiter;
    public String message;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(" - ");
        sb.append(Chrono.getTimeStr(timestamp, "dd/MM/YY HH:mm:ss")).append(" - ");
        sb.append(commiter).append(" - ");
        sb.append(message);
        return sb.toString();
    }
    
}

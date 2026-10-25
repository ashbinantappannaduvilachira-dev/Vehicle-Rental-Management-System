
package com.mycompany.vehiclerentalsystem;

import javax.swing.SwingUtilities;

public class VehicleRentalSystem {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}

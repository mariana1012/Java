import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Simulador de Financiamento de Veículos
 * Aplicação Java Swing para uma concessionária simular o financiamento
 * de veículos, novos ou usados.
 */
public class SimuladorFinanciamento extends JFrame {

    // ---- Painel Veículo ----
    private JComboBox<String> cbMarca;
    private JTextField txtModelo;
    private JComboBox<Integer> cbAno;
    private JTextField txtValor;

    // ---- Tipo (Novo / Usado) ----
    private JRadioButton rbNovo;
    private JRadioButton rbUsado;
    private ButtonGroup grupoTipo;

    // ---- Painel Veículo Usado ----
    private JPanel painelUsado;
    private JTextField txtQuilometragem;
    private JTextField txtProprietarios;

    // ---- Painel Financiamento ----
    private JCheckBox chkPossuiEntrada;
    private JLabel lblEntrada;
    private JTextField txtEntrada;
    private JComboBox<Integer> cbParcelas;

    // ---- Botões e resultado ----
    private JButton btnLimpar;
    private JButton btnCalcular;
    private JTextArea txtResultado;

    public SimuladorFinanciamento() {
        super("Simulador de Financiamento de Veículos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        add(criarPainelVeiculo(), BorderLayout.NORTH);
        add(criarPainelCentral(), BorderLayout.CENTER);
        add(criarPainelBotoesEResultado(), BorderLayout.SOUTH);

        atualizarVisibilidadeUsado();
        atualizarVisibilidadeEntrada();

        pack();
        setMinimumSize(new Dimension(480, getHeight()));
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    // Painel com os dados do veículo (Marca, Modelo, Ano, Valor)
    // Usa GridLayout para alinhar rótulos e campos em grade.
    // ------------------------------------------------------------------
    private JPanel criarPainelVeiculo() {
        JPanel painel = new JPanel(new GridLayout(4, 2, 8, 8));
        painel.setBorder(new TitledBorder("Dados do Veículo"));

        String[] marcas = {"Chevrolet", "Fiat", "Ford", "Honda", "Hyundai",
                "Renault", "Toyota", "Volkswagen"};
        cbMarca = new JComboBox<>(marcas);

        txtModelo = new JTextField();

        cbAno = new JComboBox<>();
        for (int ano = 2026; ano >= 2000; ano--) {
            cbAno.addItem(ano);
        }

        txtValor = new JTextField();

        painel.add(new JLabel("Marca:"));
        painel.add(cbMarca);
        painel.add(new JLabel("Modelo:"));
        painel.add(txtModelo);
        painel.add(new JLabel("Ano:"));
        painel.add(cbAno);
        painel.add(new JLabel("Valor do veículo (R$):"));
        painel.add(txtValor);

        return painel;
    }

    // ------------------------------------------------------------------
    // Painel central: tipo do veículo + painel condicional de usado
    // + painel de financiamento. Usa BoxLayout (eixo Y) para empilhar
    // os blocos verticalmente.
    // ------------------------------------------------------------------
    private JPanel criarPainelCentral() {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));

        painel.add(criarPainelTipo());
        painel.add(Box.createVerticalStrut(8));
        painel.add(criarPainelUsado());
        painel.add(Box.createVerticalStrut(8));
        painel.add(criarPainelFinanciamento());

        return painel;
    }

    // Painel de seleção Novo / Usado - usa FlowLayout
    private JPanel criarPainelTipo() {
        JPanel painel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painel.setBorder(new TitledBorder("Condição do Veículo"));

        rbNovo = new JRadioButton("Novo", true);
        rbUsado = new JRadioButton("Usado");
        grupoTipo = new ButtonGroup();
        grupoTipo.add(rbNovo);
        grupoTipo.add(rbUsado);

        ItemListener listener = e -> atualizarVisibilidadeUsado();
        rbNovo.addItemListener(listener);
        rbUsado.addItemListener(listener);

        painel.add(rbNovo);
        painel.add(rbUsado);
        return painel;
    }

    // Painel exibido apenas quando "Usado" está selecionado - GridLayout
    private JPanel criarPainelUsado() {
        painelUsado = new JPanel(new GridLayout(2, 2, 8, 8));
        painelUsado.setBorder(new TitledBorder("Dados do Veículo Usado"));

        txtQuilometragem = new JTextField();
        txtProprietarios = new JTextField();

        painelUsado.add(new JLabel("Quilometragem (km):"));
        painelUsado.add(txtQuilometragem);
        painelUsado.add(new JLabel("Nº de proprietários:"));
        painelUsado.add(txtProprietarios);

        return painelUsado;
    }

    // Painel de financiamento - usa GridBagLayout para maior controle
    private JPanel criarPainelFinanciamento() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(new TitledBorder("Condições de Financiamento"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        chkPossuiEntrada = new JCheckBox("Possui entrada?");
        chkPossuiEntrada.addItemListener(e -> atualizarVisibilidadeEntrada());

        lblEntrada = new JLabel("Valor de entrada (R$):");
        txtEntrada = new JTextField();

        cbParcelas = new JComboBox<>(new Integer[]{12, 24, 36, 48, 60});

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        painel.add(chkPossuiEntrada, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        painel.add(lblEntrada, gbc);
        gbc.gridx = 1;
        painel.add(txtEntrada, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        painel.add(new JLabel("Quantidade de parcelas:"), gbc);
        gbc.gridx = 1;
        painel.add(cbParcelas, gbc);

        return painel;
    }

    // Painel inferior: botões Limpar/Calcular + área de resultado
    private JPanel criarPainelBotoesEResultado() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnLimpar = new JButton("Limpar");
        btnCalcular = new JButton("Calcular");
        btnLimpar.addActionListener(e -> limparFormulario());
        btnCalcular.addActionListener(e -> calcularFinanciamento());
        painelBotoes.add(btnLimpar);
        painelBotoes.add(btnCalcular);

        txtResultado = new JTextArea(6, 30);
        txtResultado.setEditable(false);
        txtResultado.setBorder(new TitledBorder("Resultado da Simulação"));

        painel.add(painelBotoes, BorderLayout.NORTH);
        painel.add(new JScrollPane(txtResultado), BorderLayout.CENTER);

        return painel;
    }

    // ------------------------------------------------------------------
    // Regras de exibição condicional
    // ------------------------------------------------------------------
    private void atualizarVisibilidadeUsado() {
        painelUsado.setVisible(rbUsado.isSelected());
        painelUsado.revalidate();
        painelUsado.getParent().doLayout();
    }

    private void atualizarVisibilidadeEntrada() {
        boolean possuiEntrada = chkPossuiEntrada.isSelected();
        lblEntrada.setVisible(possuiEntrada);
        txtEntrada.setVisible(possuiEntrada);
        if (!possuiEntrada) {
            txtEntrada.setText("");
        }
    }

    // ------------------------------------------------------------------
    // Limpar formulário
    // ------------------------------------------------------------------
    private void limparFormulario() {
        cbMarca.setSelectedIndex(0);
        txtModelo.setText("");
        cbAno.setSelectedIndex(0);
        txtValor.setText("");

        rbNovo.setSelected(true);
        atualizarVisibilidadeUsado();

        txtQuilometragem.setText("");
        txtProprietarios.setText("");

        chkPossuiEntrada.setSelected(false);
        txtEntrada.setText("");
        atualizarVisibilidadeEntrada();
        cbParcelas.setSelectedIndex(0);

        txtResultado.setText("");
    }

    // ------------------------------------------------------------------
    // Validação + cálculo do financiamento
    // ------------------------------------------------------------------
    private void calcularFinanciamento() {
        StringBuilder erros = new StringBuilder();

        String modelo = txtModelo.getText().trim();
        if (modelo.isEmpty()) {
            erros.append("- Informe o Modelo do veículo.\n");
        }

        double valorVeiculo = 0;
        String valorTexto = txtValor.getText().trim().replace(",", ".");
        if (valorTexto.isEmpty()) {
            erros.append("- Informe o Valor do veículo.\n");
        } else {
            try {
                valorVeiculo = Double.parseDouble(valorTexto);
                if (valorVeiculo <= 0) {
                    erros.append("- O Valor do veículo deve ser maior que zero.\n");
                }
            } catch (NumberFormatException ex) {
                erros.append("- O Valor do veículo deve ser numérico.\n");
            }
        }

        boolean usado = rbUsado.isSelected();
        double quilometragem = 0;
        int proprietarios = 0;
        if (usado) {
            String kmTexto = txtQuilometragem.getText().trim().replace(",", ".");
            if (kmTexto.isEmpty()) {
                erros.append("- Informe a Quilometragem do veículo usado.\n");
            } else {
                try {
                    quilometragem = Double.parseDouble(kmTexto);
                    if (quilometragem < 0) {
                        erros.append("- A Quilometragem não pode ser negativa.\n");
                    }
                } catch (NumberFormatException ex) {
                    erros.append("- A Quilometragem deve ser numérica.\n");
                }
            }

            String propTexto = txtProprietarios.getText().trim();
            if (propTexto.isEmpty()) {
                erros.append("- Informe o número de Proprietários.\n");
            } else {
                try {
                    proprietarios = Integer.parseInt(propTexto);
                    if (proprietarios < 0) {
                        erros.append("- O número de Proprietários não pode ser negativo.\n");
                    }
                } catch (NumberFormatException ex) {
                    erros.append("- O número de Proprietários deve ser um número inteiro.\n");
                }
            }
        }

        boolean possuiEntrada = chkPossuiEntrada.isSelected();
        double valorEntrada = 0;
        if (possuiEntrada) {
            String entradaTexto = txtEntrada.getText().trim().replace(",", ".");
            if (entradaTexto.isEmpty()) {
                erros.append("- Informe o Valor de entrada.\n");
            } else {
                try {
                    valorEntrada = Double.parseDouble(entradaTexto);
                    if (valorEntrada < 0) {
                        erros.append("- O Valor de entrada não pode ser negativo.\n");
                    } else if (valorEntrada >= valorVeiculo && valorVeiculo > 0) {
                        erros.append("- O Valor de entrada deve ser menor que o Valor do veículo.\n");
                    }
                } catch (NumberFormatException ex) {
                    erros.append("- O Valor de entrada deve ser numérico.\n");
                }
            }
        }

        if (erros.length() > 0) {
            JOptionPane.showMessageDialog(this,
                    "Corrija os seguintes campos:\n\n" + erros,
                    "Dados inválidos",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // ---- Cálculo do financiamento ----
        // valor_financiado = valor_veiculo - entrada
        double valorFinanciado = valorVeiculo - valorEntrada;
        int parcelas = (int) cbParcelas.getSelectedItem();
        double valorParcela = valorFinanciado / parcelas;

        String marca = (String) cbMarca.getSelectedItem();
        int ano = (int) cbAno.getSelectedItem();

        StringBuilder resultado = new StringBuilder();
        resultado.append(String.format("Veículo: %s %s (%d) - %s%n",
                marca, modelo, ano, usado ? "Usado" : "Novo"));
        if (usado) {
            resultado.append(String.format("Quilometragem: %.0f km | Proprietários: %d%n",
                    quilometragem, proprietarios));
        }
        resultado.append(String.format("Valor do veículo: R$ %.2f%n", valorVeiculo));
        resultado.append(String.format("Valor de entrada: R$ %.2f%n", valorEntrada));
        resultado.append(String.format("Valor financiado: R$ %.2f%n", valorFinanciado));
        resultado.append(String.format("Quantidade de parcelas: %d%n", parcelas));
        resultado.append(String.format("Valor de cada parcela: R$ %.2f%n", valorParcela));

        txtResultado.setText(resultado.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new SimuladorFinanciamento().setVisible(true);
        });
    }
}

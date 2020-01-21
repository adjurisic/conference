package alfatec.controller.main;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.controlsfx.control.PrefixSelectionComboBox;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTabPane;

import alfatec.controller.email.GroupCallController;
import alfatec.controller.email.SendEmailController;
import alfatec.controller.user.ChangePasswordController;
import alfatec.controller.utils.ClearPopUp;
import alfatec.dao.conference.ConferenceDAO;
import alfatec.dao.country.CountryDAO;
import alfatec.dao.person.AuthorDAO;
import alfatec.dao.relationship.ConferenceCallDAO;
import alfatec.dao.user.UserDAO;
import alfatec.dao.utils.Logging;
import alfatec.model.country.Country;
import alfatec.model.enums.Institution;
import alfatec.model.person.Author;
import alfatec.model.relationship.ConferenceCall;
import alfatec.model.user.LoginData;
import alfatec.model.user.User;
import alfatec.view.gui.MainView;
import alfatec.view.utils.GUIUtils;
import alfatec.view.utils.Utility;
import database.DatabaseUtility;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainInterfaceController extends GUIUtils implements Initializable {

	@FXML
	private Button quitButton, minimizeButton, closePopupButton, clearPopupButton;

	@FXML
	private Label welcomeLabel, firstNameLabel, lastNameLabel, emailLabel, institutionLabel, institutionNameLabel,
			countryLabel, emailErrorLabel;

	@FXML
	private Hyperlink changePasswordHyperlink, logoutHyperlink;

	@FXML
	private JFXTabPane tabPane;

	@FXML
	private TextField searchAuthorTextField, firstNameTextField, lastNameTextField, emailTextField,
			institutionNameTextField;

	@FXML
	private TableView<Author> authorsTableView;

	@FXML
	private TableColumn<Author, String> authorColumn, emailColumn;

	@FXML
	private JFXButton addAuthorButton, editAuthorButton, deleteAuthorButton, saveAuthorButton, firstInviteButton,
			secondInviteButton, thirdInvButton, sendEmailButton;

	@FXML
	private HBox authorDetailsHbox, invitesHbox;

	@FXML
	private VBox popupVbox, detailsVbox;

	@FXML
	private JFXRadioButton firstRadio, secondRadio, thirdRadio;

	@FXML
	private JFXCheckBox interestedCheckbox;

	@FXML
	private ComboBox<Institution> institutionComboBox;

	@FXML
	private PrefixSelectionComboBox<Country> countryComboBox;

	@FXML
	private TextArea noteTextArea, noteTextAreaPreview;

	@FXML
	private ToggleGroup group;

	private ChangePasswordController changePasswordController;
	private SendEmailController send;
	private GroupCallController groupCall;
	private ObservableList<Author> authorsData;
	private String email;
	private Author author;
	private LoginData loginData;
	private ConferenceCall call;
	private ClearPopUp popup;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		popup = () -> {
			clearFields(Arrays.asList(firstNameTextField, lastNameTextField, emailTextField, institutionNameTextField),
					Arrays.asList(emailErrorLabel));
			noteTextArea.clear();
			setUpBoxes();
		};
		authorsData = AuthorDAO.getInstance().getAllAuthors();
		populateAuthorTable();
		handleSearch();
		setUpDetails();
	}

	@FXML
	private void addAuthor(ActionEvent event) {
		setUpBoxes();
		if (isPopupOpen())
			closePopup(popupVbox, 520, popup);
		setAddAction(true);
		openPopup(popupVbox, 940);
	}

	@FXML
	private void editAuthor() {
		if (isPopupOpen())
			closePopup(popupVbox, 520, popup);
		setEditAction(true);
		author = authorsTableView.getSelectionModel().getSelectedItem();
		if (author != null) {
			setAuthor(author);
			openPopup(popupVbox, 520);
		}
	}

	@FXML
	private void deleteAuthor(ActionEvent event) {
		author = authorsTableView.getSelectionModel().getSelectedItem();
		if (author != null) {
			ButtonType bt = confirmationAlert("Please confirm:", "Are you sure you want to delete data for "
					+ author.getAuthorFirstName() + " " + author.getAuthorLastName() + "?", AlertType.CONFIRMATION);
			if (bt == ButtonType.OK) {
				AuthorDAO.getInstance().deleteAuthor(author);
				AuthorDAO.getInstance().getAllAuthors().remove(author);
				authorsData.remove(author);
				authorsTableView.getItems().remove(author);
				refresh(authorsTableView, authorsTableView.getSelectionModel().getSelectedIndex() - 1, popupVbox, 520,
						popup);
				closeDetails(authorDetailsHbox, 1200);
			}
		}
	}

	@FXML
	private void saveAuthor(ActionEvent event) {
		if (isAddAction()) {
			setAddAction(false);
			if (isValidInput() && !isEmailAlreadyInDB()) {
				authorsData.add(getNewAuthor());
				refresh(authorsTableView, authorsData.size() - 1, popupVbox, 520, popup);
				closeDetails(authorDetailsHbox, 1200);
				openDetails(authorDetailsHbox, 1200);
				showAuthor(author);
			} else if (isEmailAlreadyInDB())
				emailErrorLabel.setText("E-mail already exists in database.");
		} else if (isEditAction()) {
			setEditAction(false);
			author = authorsTableView.getSelectionModel().getSelectedItem();
			if (isValidInput())
				if (!emailTextField.getText().equals(email) && isEmailAlreadyInDB())
					emailErrorLabel.setText("Database already has enter with the same e-mail address.");
				else {
					handleEditAuthor();
					refresh(authorsTableView, authorsTableView.getSelectionModel().getSelectedIndex(), popupVbox, 520,
							popup);
					showAuthor(author);
				}
		}
	}

	@FXML
	private void sendEmail() {
		author = authorsTableView.getSelectionModel().getSelectedItem();
		if (author != null)
			send = MainView.getInstance().loadEmailWindow(send, author.getAuthorEmail());
	}

	@FXML
	private void sendFirstInvite() {
		List<String> list = new ArrayList<String>();
		for (Author a : authorsData)
			list.add(a.getAuthorEmail());
		groupCall = MainView.getInstance().loadEmailWindow(groupCall, list);
		groupCall.setRecievers(list);
		if (groupCall.isSent())
			for (Author a : authorsData)
				ConferenceCallDAO.getInstance().createEntry(
						ConferenceDAO.getInstance().getCurrentConference().getConferenceID(), a.getAuthorID());
	}

	@FXML
	private void sendSecondInvite() {
		List<String> list = new ArrayList<String>();
		for (Author a : authorsData) {
			ConferenceCall call = ConferenceCallDAO.getInstance().getCurrentAnswer(a.getAuthorID());
			if (call != null && !call.isFirstCallAnswered() && call.isInterested())
				list.add(a.getAuthorEmail());
		}
		groupCall = MainView.getInstance().loadEmailWindow(groupCall, list);
	}

	@FXML
	private void sendThirdInvite() {
		List<String> list = new ArrayList<String>();
		for (Author a : authorsData) {
			ConferenceCall call = ConferenceCallDAO.getInstance().getCurrentAnswer(a.getAuthorID());
			if (call != null && !call.isSecondCallAnswered() && call.isInterested())
				list.add(a.getAuthorEmail());
		}
		groupCall = MainView.getInstance().loadEmailWindow(groupCall, list);
	}

	@FXML
	private void changePassword(ActionEvent event) {
		changePasswordController = MainView.getInstance().loadChangePassword(changePasswordController, loginData);
	}

	@FXML
	private void minimizeApp() {
		Stage stage = (Stage) minimizeButton.getScene().getWindow();
		stage.setIconified(true);
	}

	@FXML
	private void logout(ActionEvent event) {
		MainView.getInstance().closeMainView(event);
	}

	@FXML
	private void quitApp() {
		DatabaseUtility.getInstance().databaseDisconnect();
		Platform.exit();
	}

	@FXML
	private void clearPopup(ActionEvent event) {
		popup.clear();
	}

	@FXML
	private void closePopup(ActionEvent event) {
		closePopup(popupVbox, 520, popup);
	}

	public void setWelcomeMessage(LoginData ld) {
		User user = UserDAO.getInstance().getUser(ld.getUserID());
		welcomeLabel.setText("Welcome, " + user.getUserFirstName());
	}

	public void setLoginData(LoginData data) {
		this.loginData = data;
	}

	public void loadTabs(LoginData lgData) {
		if (lgData.getRoleID() != 1)
			MainView.getInstance().loadTabs(tabPane, lgData);
	}

	public void disableOptionsForUsers(LoginData lgData) {
		if (lgData.getRoleID() == 1 || ConferenceDAO.getInstance().getCurrentConference() == null) {
			sendEmailButton.setVisible(false);
			detailsVbox.setDisable(true);
			invitesHbox.setVisible(false);
		}
	}

	private void populateAuthorTable() {
		authorsTableView.setPlaceholder(new Label("Database table \"author\" is empty"));
		Utility.setUpStringCell(authorsTableView);
		authorColumn.setCellValueFactory(cellData -> cellData.getValue().getAuthorFirstNameProperty().concat(" ")
				.concat(cellData.getValue().getAuthorLastNameProperty()));
		emailColumn.setCellValueFactory(cellData -> cellData.getValue().getAuthorEmailProperty());
		authorsTableView.setItems(authorsData);
		authorsTableView.setOnMousePressed(event -> {
			if (event.getButton() == MouseButton.PRIMARY)
				if (authorsTableView.getSelectionModel().getSelectedItem() != null) {
					if (isPopupOpen())
						closePopup(popupVbox, 520, popup);
					openDetails(authorDetailsHbox, 1200);
					showAuthor(authorsTableView.getSelectionModel().getSelectedItem());
				}
		});
	}

	private void handleSearch() {
		searchAuthorTextField.setOnKeyTyped(event -> {
			String search = searchAuthorTextField.getText();
			Pattern pattern = Pattern.compile("[@()\\\\<>+~%\\*\\-\\'\"]");
			Matcher matcher = pattern.matcher(search);
			if (search.length() > 0 && !matcher.find()) {
				ObservableList<Author> searched = AuthorDAO.getInstance().searchForAuthors(search);
				authorsTableView.getItems().setAll(searched);
			} else {
				authorsData = AuthorDAO.getInstance().getAllAuthors();
				authorsTableView.getItems().setAll(authorsData);
			}
		});
	}

	private void setAuthor(Author author) {
		this.author = author;
		firstNameTextField.setText(author.getAuthorFirstName());
		lastNameTextField.setText(author.getAuthorLastName());
		emailTextField.setText(author.getAuthorEmail());
		countryComboBox.getSelectionModel().select(author.getCountryID() - 1);
		institutionComboBox.getSelectionModel().select(author.getInstitution());
		institutionNameTextField.setText(author.getInstitutionName());
		noteTextArea.setText(author.getNote());
	}

	private void showAuthor(Author author) {
		call = ConferenceCallDAO.getInstance().getCurrentAnswer(author.getAuthorID());
		email = author.getAuthorEmail();
		authorDetailsHbox.setVisible(true);
		firstNameLabel.setText(author.getAuthorFirstName() + " " + author.getAuthorLastName());
		emailLabel.setText(email);
		institutionLabel.setText(author.getInstitution() == null ? "" : author.getInstitution().name());
		institutionNameLabel.setText(author.getInstitutionName());
		countryLabel.setText(author.countryProperty() == null ? "" : author.countryProperty().get());
		noteTextAreaPreview.setText(author.getNoteProperty().get());
		showDetails();
	}

	private Author getNewAuthor() {
		if (isValidInput()) {
			String country = countryComboBox.getSelectionModel().getSelectedItem() != null
					? countryComboBox.getSelectionModel().getSelectedItem().getCountryName()
					: null;
			String institution = institutionComboBox.getSelectionModel().getSelectedItem() != null
					? institutionComboBox.getSelectionModel().getSelectedItem().name().toLowerCase()
					: null;
			author = AuthorDAO.getInstance().createAuthor(firstNameTextField.getText(), lastNameTextField.getText(),
					emailTextField.getText(), country, institution, institutionNameTextField.getText(),
					noteTextArea.getText());
			Logging.getInstance().change("create", "Create author:\n\t" + author.getAuthorEmail());
		}
		return author;
	}

	private void handleEditAuthor() {
		setFirstName();
		setLastName();
		setEmail();
		setCountry();
		setInstitutionType();
		setInstitutionName();
		setNote();
		authorsTableView.refresh();
	}

	private boolean isValidInput() {
		emailErrorLabel.setText(isValidEmail(emailTextField) ? "" : "Empty or invalid email field.");
		return isValidEmail(emailTextField);
	}

	private boolean isEmailAlreadyInDB() {
		return AuthorDAO.getInstance().findAuthorByExactEmail(emailTextField.getText()) != null;
	}

	private void setFirstName() {
		if (!firstNameTextField.getText().equalsIgnoreCase(author.getAuthorFirstName()))
			AuthorDAO.getInstance().updateAuthorFirstName(author, firstNameTextField.getText());
	}

	private void setLastName() {
		if (!lastNameTextField.getText().equalsIgnoreCase(author.getAuthorLastName()))
			AuthorDAO.getInstance().updateAuthorLastName(author, lastNameTextField.getText());
	}

	private void setEmail() {
		if (!emailTextField.getText().equalsIgnoreCase(author.getAuthorEmail()))
			AuthorDAO.getInstance().updateAuthorEmail(author, emailTextField.getText());
	}

	private void setInstitutionType() {
		if ((institutionComboBox.getSelectionModel().getSelectedItem() != null && author.getInstitution() == null)
				|| (institutionComboBox.getSelectionModel().getSelectedItem() != null && author.getInstitution() != null
						&& !institutionComboBox.getSelectionModel().getSelectedItem().name()
								.equalsIgnoreCase(author.getInstitution().name())))
			AuthorDAO.getInstance().updateAuthorInstitution(author,
					institutionComboBox.getSelectionModel().getSelectedItem().name().toLowerCase());
	}

	private void setInstitutionName() {
		if (institutionNameTextField.getText() == null)
			return;
		if (!institutionNameTextField.getText().equals(author.getInstitutionName()))
			AuthorDAO.getInstance().updateAuthorInstitutionName(author, institutionNameTextField.getText());
	}

	private void setCountry() {
		if (countryComboBox.getSelectionModel().getSelectedItem() != null
				&& countryComboBox.getSelectionModel().getSelectedItem().getCountryID() != author.getCountryID())
			AuthorDAO.getInstance().updateAuthorCountry(author,
					countryComboBox.getSelectionModel().getSelectedItem().getCountryName());
	}

	private void setNote() {
		if (noteTextArea.getText() == null)
			return;
		if (!noteTextArea.getText().equalsIgnoreCase(author.getNote()))
			AuthorDAO.getInstance().updateAuthorNote(author, noteTextArea.getText());
	}

	private void setUpBoxes() {
		institutionComboBox.getItems().setAll(FXCollections.observableArrayList(Institution.values()));
		countryComboBox.getItems()
				.setAll(FXCollections.observableArrayList(CountryDAO.getInstance().getAllCountries()));
		institutionComboBox.setPromptText("Please select");
		countryComboBox.setPromptText("Please select");
	}

	private void setUpFields() {
		setUpFields(new TextField[] { firstNameTextField, lastNameTextField, emailTextField, institutionNameTextField },
				new int[] { getFirstNameLength(), getLastNameLength(), getEmailLength(), getInstitutionNameLength() });
		setUpFields(new TextArea[] { noteTextArea }, new int[] { getNoteLength() });
		noteTextArea.setWrapText(true);
		noteTextAreaPreview.setWrapText(true);
	}

	private void setUpDetails() {
		setUpBoxes();
		setUpFields();
		group.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
			try {
				if ((newValue == firstRadio && !call.isFirstCallAnswered()) || oldValue == firstRadio)
					ConferenceCallDAO.getInstance().updateFirstCall(call.getAuthorID(), newValue == firstRadio);
				if ((newValue == secondRadio && !call.isSecondCallAnswered()) || oldValue == secondRadio)
					ConferenceCallDAO.getInstance().updateSecondCall(call.getAuthorID(), newValue == secondRadio);
				if ((newValue == thirdRadio && !call.isThirdCallAnswered()) || oldValue == thirdRadio)
					ConferenceCallDAO.getInstance().updateThirdCall(call.getAuthorID(), newValue == thirdRadio);
			} catch (NullPointerException e) {
				group.getToggles().forEach(button -> button.setSelected(false));
			}
		});
		interestedCheckbox.setOnAction(event -> {
			try {
				ConferenceCallDAO.getInstance().updateInterested(call.getAuthorID(), interestedCheckbox.isSelected());
			} catch (NullPointerException e) {
				interestedCheckbox.setSelected(false);
			}
		});
	}

	private void showRadioButton(JFXRadioButton button) {
		try {
			button.setSelected(button == firstRadio ? call.isFirstCallAnswered()
					: button == secondRadio ? call.isSecondCallAnswered() : call.isThirdCallAnswered());
		} catch (NullPointerException e) {
			button.setSelected(false);
		}
	}

	private void showCheckBox() {
		try {
			interestedCheckbox.setSelected(call.isInterested());
		} catch (NullPointerException e) {
			interestedCheckbox.setSelected(false);
		}
	}

	private void showDetails() {
		showRadioButton(firstRadio);
		showRadioButton(secondRadio);
		showRadioButton(thirdRadio);
		showCheckBox();
	}
}
